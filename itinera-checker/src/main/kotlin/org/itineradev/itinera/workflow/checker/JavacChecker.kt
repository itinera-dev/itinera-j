package org.itineradev.itinera.workflow.checker

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

/**
 * itinera's compile-time checks for Java: an annotation processor that reports, as compile errors,
 * every mistake in an annotated workflow class that `WorkflowDescriptor.of` would refuse when the
 * workflow is described, such as a hook declared to return what its kind does not, or a step whose
 * service no provider serves. javac runs it for an application that adds `itinera-checker` to its
 * annotation processors, with `annotationProcessor("org.itinera-dev:itinera-checker")` in Gradle.
 *
 * It checks each workflow class javac compiles, and the step, rescue step, policy and reporter
 * classes it names, wherever they were compiled, after the last round, so that it reads the classes
 * other processors generate, and only when javac knows every class the workflow's annotations name.
 * What only the running application can tell is left to the run-time checks: whether a module opens
 * its packages to itinera, whether the application has `itinera-java` or `itinera-kotlin`, and
 * whether a class was compiled with `-parameters`. So are classes compiled from Kotlin, which javac
 * reads only as Java reflection does: a workflow is not checked when it, or a part it names,
 * extends one or is one, and a class compiled from Kotlin that a request names is taken to be data.
 *
 * @sample org.itineradev.itinera.workflow.checker.samples.checkingJava
 */
public class JavacChecker : AbstractProcessor() {
    private val reported = HashSet<Pair<String, Any>>()
    private val compiled = LinkedHashSet<String>()

    /** Every annotation, so that javac hands the checker every class it compiles. */
    override fun getSupportedAnnotationTypes(): Set<String> = setOf("*")

    /** The newest version of Java that the running javac knows. */
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    /**
     * Gathers the classes of [round], and after the last round reports each mistake in their
     * workflow classes as a compile error. Claims none of [annotations], so other processors still
     * see them.
     */
    override fun process(annotations: Set<TypeElement>, round: RoundEnvironment): Boolean {
        ElementFilter.typesIn(round.rootElements)
            .flatMap(::withNested)
            .filter(::isConcrete)
            .mapTo(compiled, ::qualifiedNameOf)
        if (round.processingOver()) check()
        return false
    }

    private fun check() {
        val elements = processingEnv.elementUtils
        val symbols = JavacSymbols(elements, processingEnv.typeUtils)
        compiled
            .mapNotNull(elements::getTypeElement)
            .filter(::namesOnlyKnownClasses)
            .map(symbols::classOf)
            .filterNot(::namesKotlinPart)
            .flatMap(::mistakesOf)
            .forEach(::report)
    }

    private fun report(finding: Finding) {
        if (reported.add(finding.message to finding.origin))
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                finding.message,
                finding.origin as Element,
            )
    }
}

/** Every mistake of [type], once for each mode whose workflow interface it implements. */
internal fun mistakesOf(type: ClassSymbol): List<Finding> =
    Mode.entries
        .filter { mode -> type.isSubtypeOf(mode.workflow) }
        .flatMap { mode -> mistakesIn(type, mode) }

private fun mistakesIn(type: ClassSymbol, mode: Mode): List<Finding> =
    Reading(type, mode).mistakes()

private fun withNested(type: TypeElement): List<TypeElement> =
    listOf(type) + ElementFilter.typesIn(type.enclosedElements).flatMap(::withNested)

private fun qualifiedNameOf(type: TypeElement): String = type.qualifiedName.toString()

private fun isConcrete(type: TypeElement): Boolean =
    type.kind.isClass && Modifier.ABSTRACT !in type.modifiers
