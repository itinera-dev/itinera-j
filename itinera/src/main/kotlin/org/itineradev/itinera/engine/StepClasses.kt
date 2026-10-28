package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Violation

/** Reads step classes into [StepClass]es of [mode], recording each mistake in [problems]. */
internal class StepClasses<M : Mode>(
    private val mode: M,
    private val factories: StepFactories<M>,
    private val problems: Problems,
) {
    private val requests = StepRequests(factories, problems)

    /** The step class [step] named [name], or `null` when the class has a mistake. */
    fun read(name: StepName, step: Class<*>): StepClass<M>? {
        val buildable = problems.readable(step) && marked(step) && buildable(step, problems)
        val constructor = if (buildable) constructorOf(step, "a step", problems) else null
        val run =
            if (buildable) runOf(step, problems)?.takeIf { run -> checked(name, run) } else null
        val inputs = constructor?.let(requests::inputs)
        val asks = run?.let(requests::ofRun)
        return if (constructor != null && run != null)
            stepClass(step, constructor, inputs, run, asks)
        else null
    }

    private fun stepClass(
        step: Class<*>,
        constructor: Constructor<*>,
        inputs: List<Request<StepKind<M>, *>>?,
        run: Method,
        asks: List<Request<StepKind<M>, *>>?,
    ): StepClass<M>? =
        if (inputs != null && asks != null && problems.opened(step, constructor, run))
            StepClass(constructor, inputs, run, asks)
        else null

    private fun marked(step: Class<*>): Boolean {
        val marked = step.isAnnotationPresent(Step::class.java)
        if (!marked) problems.misdeclared(step.name, "is not marked @Step")
        return marked
    }

    private fun checked(name: StepName, run: Method): Boolean {
        val function = functionOf(run)
        val inMode = function.isSuspend == (mode == Async)
        if (!inMode) problems += Violation.ModeNotAccepted("step $name", mode)
        return inMode && problems.returns(run, function, Outcome::class.java)
    }
}

/**
 * A step class as a workflow class's `@WithStep` declares it: its one constructor, which takes its
 * inputs, and its method `run`, which takes its services and handles.
 *
 * Part of itinera's engine, not of its API.
 */
public class StepClass<M : Mode>
internal constructor(
    /** The constructor. */
    internal val constructor: Constructor<*>,
    private val inputs: List<Request<StepKind<M>, *>>,
    /** The method `run`. */
    public val run: Method,
    private val asks: List<Request<StepKind<M>, *>>,
) {
    /** What the step requests: its constructor's, then its run's. */
    internal val requests: List<Request<StepKind<M>, *>>
        get() = inputs + asks

    /** The step built for one attempt, with its constructor's answers in [got]. */
    public fun built(got: Got<StepKind<M>>): Any = constructed(constructor, *answers(inputs, got))

    /** The arguments its run is called with, from its answers in [got]. */
    public fun arguments(got: Got<StepKind<M>>): Array<Any?> = answers(asks, got)
}

/** Whether [part], a step's or a rescue step's class, can be built, recording why not. */
internal fun buildable(part: Class<*>, problems: Problems): Boolean {
    val abstract = Modifier.isAbstract(part.modifiers)
    if (abstract) problems.misdeclared(part.name, "cannot be built: it is abstract")
    return !abstract
}

/** The one constructor of [part], which [what], such as `a step`, has, recording when not one. */
internal fun constructorOf(part: Class<*>, what: String, problems: Problems): Constructor<*>? {
    val constructors = part.declaredConstructors.filterNot(Constructor<*>::isSynthetic)
    if (constructors.size != 1)
        problems.misdeclared(part.name, "has several constructors, and $what has one")
    return constructors.singleOrNull()
}

/** The one public method `run` of [part], recording when there is not one. */
internal fun runOf(part: Class<*>, problems: Problems): Method? {
    val runs = part.methods.filter(::isRun)
    if (runs.size != 1) {
        val count = if (runs.isEmpty()) "no public method" else "several public methods"
        problems.misdeclared(part.name, "has $count named run")
    }
    return runs.singleOrNull()
}

/** The answers in [got] to [requests], in order, as the arguments of a call. */
internal fun <K> answers(requests: List<Request<K, *>>, got: Got<K>): Array<Any?> =
    requests.map { request -> got[request] }.toTypedArray()

private fun isRun(method: Method): Boolean =
    method.name == "run" && !method.isBridge && !method.isSynthetic
