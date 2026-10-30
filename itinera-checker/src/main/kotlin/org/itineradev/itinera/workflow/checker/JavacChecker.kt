package org.itineradev.itinera.workflow.checker

import com.sun.source.tree.CompilationUnitTree
import com.sun.source.tree.Tree
import com.sun.source.util.JavacTask
import com.sun.source.util.Plugin
import com.sun.source.util.TaskEvent
import com.sun.source.util.TaskListener
import com.sun.source.util.TreePath
import com.sun.source.util.Trees
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

/**
 * itinera's compile-time checks for Java: a javac plugin that reports, as compile errors, every
 * mistake in an annotated workflow class that `WorkflowDescriptor.of` would refuse when the
 * workflow is described, such as a hook declared to return what its kind does not, or a step whose
 * service no provider serves. javac starts it on its own for an application that adds
 * `itinera-checker` to its annotation processor path, with
 * `annotationProcessor("org.itinera-dev:itinera-checker")` in Gradle, with no other option.
 *
 * It is no annotation processor, so it claims no annotation and hides none from the processors an
 * application runs, and gives javac's `processing` lint nothing to warn of: an application without
 * other annotation processors may compile with `-Xlint:all -Werror`. When another processor runs,
 * that lint still warns of each annotation no processor claims, itinera's among them, which
 * `-Xlint:all,-processing` silences. It checks each workflow class among the sources javac is given
 * and those processors generate, and the step, rescue step, policy and reporter classes it names,
 * wherever they were compiled, once every annotation processor ran, so that it reads the classes
 * they generate, and before javac generates any class, and only when javac knows every class the
 * workflow's annotations name. It runs whenever javac compiles, even with `-proc:none`, but with
 * `-proc:only` only when an annotation processor runs too, and javac also starts it from the class
 * path when no processor path is given. What only the running application can tell is left to the
 * run-time checks: whether a module opens its packages to itinera, whether the application has
 * `itinera-java` or `itinera-kotlin`, and whether a class was compiled with `-parameters`. So are
 * classes compiled from Kotlin, which javac reads only as Java reflection does: a workflow is not
 * checked when it, or a part it names, extends one or is one, and a class compiled from Kotlin that
 * a request names is taken to be data.
 *
 * @sample org.itineradev.itinera.workflow.checker.samples.checkingJava
 */
public class JavacChecker : Plugin {
    /** `ItineraChecker`, the name `-Xplugin` would start it by, though javac starts it unasked. */
    override fun getName(): String = "ItineraChecker"

    /** Always, so that javac starts the checker when it finds it, with no `-Xplugin` option. */
    override fun autoStart(): Boolean = true

    /** Checks the workflow classes [task] compiles, once every annotation processor ran. */
    override fun init(task: JavacTask, vararg args: String) {
        task.addTaskListener(JavacChecking(task))
    }
}

/**
 * Gathers the classes [task] enters, its own and those annotation processors generate, and checks
 * them all once, after the last round of processing or before the first class is analysed, before
 * any class is generated, so that every mistake is reported on the declaration that makes it.
 */
private class JavacChecking(private val task: JavacTask) : TaskListener {
    private val trees = Trees.instance(task)
    private val compiled = LinkedHashSet<String>()
    private val reported = HashSet<Pair<String, Tree>>()
    private var checked = false

    override fun started(event: TaskEvent) {
        if (event.kind == TaskEvent.Kind.ANALYZE) checkOnce()
    }

    override fun finished(event: TaskEvent) {
        when (event.kind) {
            TaskEvent.Kind.ENTER -> enter(event.compilationUnit)
            TaskEvent.Kind.ANNOTATION_PROCESSING -> checkOnce()
            else -> Unit
        }
    }

    private fun enter(unit: CompilationUnitTree) {
        unit.typeDecls
            .mapNotNull { declaration -> declared(unit, declaration) }
            .flatMap(::withNested)
            .filter(::isConcrete)
            .mapTo(compiled, ::qualifiedNameOf)
    }

    private fun declared(unit: CompilationUnitTree, declaration: Tree): TypeElement? =
        trees.getElement(TreePath(TreePath(unit), declaration)) as? TypeElement

    private fun checkOnce() {
        if (checked) return
        checked = true
        val elements = task.elements
        val symbols = JavacSymbols(elements, task.types)
        for (workflow in compiled.mapNotNull(elements::getTypeElement)) {
            if (namesOnlyKnownClasses(workflow)) check(workflow, symbols)
        }
    }

    private fun check(workflow: TypeElement, symbols: JavacSymbols) {
        val type = symbols.classOf(workflow)
        if (!namesKotlinPart(type))
            mistakesOf(type).forEach { finding -> report(finding, workflow) }
    }

    /**
     * Reports [finding] where it was made, or at [workflow] when that is outside the sources, so
     * that each workflow naming a part from a library hears of its mistakes.
     */
    private fun report(finding: Finding, workflow: TypeElement) {
        val path = trees.getPath(finding.origin as Element) ?: trees.getPath(workflow)
        if (reported.add(finding.message to path.leaf)) {
            trees.printMessage(
                Diagnostic.Kind.ERROR,
                finding.message,
                path.leaf,
                path.compilationUnit,
            )
        }
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
