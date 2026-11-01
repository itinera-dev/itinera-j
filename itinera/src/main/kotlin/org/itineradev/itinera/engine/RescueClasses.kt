package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Violation

/**
 * Reads rescue step classes into [RescueClass]es of [mode], recording each mistake in [problems].
 */
internal class RescueClasses<M : Mode>(
    private val mode: M,
    factories: StepFactories<M>,
    private val problems: Problems,
) {
    private val requests = StepRequests(factories, problems)

    /** The rescue step class [rescue] of the step [step], or `null` when it has a mistake. */
    fun read(step: StepName, rescue: Class<*>): RescueClass<M>? {
        val buildable = problems.readable(rescue) && marked(rescue) && buildable(rescue, problems)
        val constructor = if (buildable) constructorOf(rescue, "a rescue step", problems) else null
        val run =
            if (buildable) runOf(rescue, problems)?.takeIf { run -> checked(step, run) } else null
        val asked = constructor?.let(requests::ofRescue)
        val asks = run?.let(requests::ofRun)
        return if (constructor != null && run != null)
            rescueClass(rescue, constructor, asked, run, asks)
        else null
    }

    private fun rescueClass(
        rescue: Class<*>,
        constructor: Constructor<*>,
        asked: List<Request<RescueKind<M>, *>>?,
        run: Method,
        asks: List<Request<StepKind<M>, *>>?,
    ): RescueClass<M>? =
        if (asked != null && asks != null && problems.opened(rescue, constructor, run))
            RescueClass(constructor, asked, run, asks)
        else null

    private fun marked(rescue: Class<*>): Boolean {
        val marked = rescue.isAnnotationPresent(RescueStep::class.java)
        if (!marked) problems.misdeclared(rescue.name, "is not marked @RescueStep")
        return marked
    }

    private fun checked(step: StepName, run: Method): Boolean {
        val function = functionOf(run)
        val inMode = function.isSuspend == (mode == Async)
        if (!inMode) problems += Violation.ModeNotAccepted("rescue step ${rescueOf(step)}", mode)
        val outcome = Outcome::class.java.isAssignableFrom(function.returnType)
        if (outcome)
            problems.misdeclared(
                run.toString(),
                "returns Outcome, a step's outcome, though a rescue step never skips: return " +
                    "RescueOutcome",
            )
        return inMode && !outcome && problems.returns(run, function, RescueOutcome::class.java)
    }
}

/**
 * A rescue step class as a workflow class's `@WithStep` declares it: its one constructor, which
 * takes its inputs and what it requests from the step it rescues, and its method `run`, which takes
 * its services and handles.
 *
 * Part of itinera's engine, not of its API.
 */
public class RescueClass<M : Mode>
internal constructor(
    private val constructor: Constructor<*>,
    private val asked: List<Request<RescueKind<M>, *>>,
    /** The method `run`. */
    public val run: Method,
    private val asks: List<Request<StepKind<M>, *>>,
) {
    /** What the rescue step requests: its constructor's, then its run's. */
    internal val requests: List<Request<RescueKind<M>, *>>
        get() = asked + asks

    /** The rescue step built, with its constructor's answers in [got]. */
    public fun built(got: Got<RescueKind<M>>): Any = constructed(constructor, *answers(asked, got))

    /** The arguments its run is called with, from its answers in [got]. */
    public fun arguments(got: Got<RescueKind<M>>): Array<Any?> = answers(asks, got)
}
