package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode

/**
 * itinera's compile-time checks for Kotlin: a KSP processor that reports, as compile errors, every
 * mistake in an annotated workflow class that describing the workflow would refuse, such as a hook
 * declared to return what its kind does not, a step without `suspend` in an asynchronous workflow,
 * or a step whose service no provider serves. KSP runs it for an application that applies the KSP
 * Gradle plugin and adds `itinera-checker` with `ksp("org.itinera-dev:itinera-checker")`.
 *
 * It checks each workflow class KSP reads in the sources being compiled that is written in Kotlin,
 * or extends a class that is, or names a part that is or does, and the step, rescue step, policy
 * and reporter classes it names, wherever they were compiled: what `JavacChecker` leaves to the run
 * time, so a workflow written in Java with Java parts only is checked once, by javac. What only the
 * running application can tell is left to the run-time checks: whether a module opens its packages
 * to itinera, and whether the application has `itinera-kotlin` or `itinera-java`.
 *
 * @sample org.itineradev.itinera.workflow.checker.samples.checkingKotlin
 */
public class KspChecker : SymbolProcessorProvider {
    /** A processor that checks the workflow classes of each round of the compilation KSP runs. */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        Checking(environment.logger)
}

/**
 * Checks the workflow classes of the files each round brings, reporting each mistake once. A class
 * whose declarations, or those of the classes its annotations name, name a type KSP cannot resolve
 * yet, such as one another processor generates in a later round, waits for the next round, and is
 * left to the compiler if it never resolves.
 */
private class Checking(private val logger: KSPLogger) : SymbolProcessor {
    private val reported = HashSet<Pair<String, Any>>()
    private val deferred = LinkedHashSet<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = KspSymbols(resolver)
        val types =
            resolver
                .getNewFiles()
                .flatMap(KSFile::declarations)
                .filterIsInstance<KSClassDeclaration>()
                .flatMap(::withNested)
                .filter(::isConcrete)
                .toList() + deferred.mapNotNull(symbols::named)
        deferred.clear()
        val (resolved, unresolved) = types.partition(::isResolved)
        unresolved.mapTo(deferred, ::binaryNameOf)
        resolved
            .map(symbols::classOf)
            .filter(::namesKotlinPart)
            .flatMap(::mistakesOf)
            .forEach(::report)
        return emptyList()
    }

    private fun report(finding: Finding) {
        if (reported.add(finding.message to finding.origin))
            logger.error(finding.message, finding.origin as KSNode)
    }
}

private fun withNested(type: KSClassDeclaration): Sequence<KSClassDeclaration> =
    sequenceOf(type) +
        type.declarations.filterIsInstance<KSClassDeclaration>().flatMap(::withNested)

private fun isConcrete(type: KSClassDeclaration): Boolean =
    type.classKind in concreteKinds && !type.isAbstract()

private val concreteKinds = setOf(ClassKind.CLASS, ClassKind.OBJECT, ClassKind.ENUM_CLASS)
