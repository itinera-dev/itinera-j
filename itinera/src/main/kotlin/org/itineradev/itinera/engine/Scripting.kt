package org.itineradev.itinera.engine

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode

/**
 * What a scripted run tells the engine: it records every attempt handed to the construction of its
 * step, and plays, for the attempts it describes, what they do in place of the step's run. Every
 * step is still built, so only the call to its run is replaced.
 *
 * Part of itinera's engine, not of its API.
 */
public interface Scripting {
    /**
     * Records that [attempt] of the step or rescue step [step] is handed to the construction of its
     * step, with the values its requests [found], by key: its inputs and, for a rescue step, the
     * contributions of the failed attempt it requested.
     */
    public fun building(step: StepName, attempt: Attempt, found: Map<String, Captured>)

    /**
     * What [attempt] of [step] plays in place of the step's run, or `null` when it runs for real.
     */
    public fun step(step: StepName, attempt: Attempt): Play<Outcome>?

    /** What the rescue step [rescue] plays in place of its run, or `null` when it runs for real. */
    public fun rescue(rescue: StepName): Play<RescueOutcome>?
}

/**
 * A scripted attempt: what it does through what the attempt lends it, then the outcome it ends
 * with, or a throw, as an error escaping a step's run.
 *
 * Part of itinera's engine, not of its API.
 */
public fun interface Play<out O> {
    /** Plays the attempt with what it is [lent]. */
    @JvmSynthetic public suspend fun play(lent: Lent): O
}

/**
 * What an attempt lends what plays it: its [contributor], and the [emits] its step reporter emits
 * and sets global metadata through.
 *
 * Part of itinera's engine, not of its API.
 */
public class Lent internal constructor(public val contributor: Contributor, public val emits: Emits)

/**
 * A workflow instance whose journey runs as [scripting] says.
 *
 * Part of itinera's engine, not of its API.
 */
public interface ScriptedInstance<M : Mode> : WorkflowInstance<M> {
    /** What the journey plays and records. */
    public val scripting: Scripting
}

/**
 * Whether [requests] hold the contributor.
 *
 * Part of itinera's engine, not of its API.
 */
public fun declaresContributor(requests: List<Request<*, *>>): Boolean =
    requests.any(::isContributor)

/**
 * Whether [requests] hold a step reporter, of either mode.
 *
 * Part of itinera's engine, not of its API.
 */
public fun declaresStepReporter(requests: List<Request<*, *>>): Boolean =
    requests.any(::isStepReporter)

private fun isContributor(request: Request<*, *>): Boolean = request.asked == Handle.CONTRIBUTOR

private fun isStepReporter(request: Request<*, *>): Boolean =
    request.asked == Handle.STEP_REPORTER || request.asked == Handle.ASYNC_STEP_REPORTER

/** The scripting of [instance], or none, when its journey runs every step for real. */
internal fun scriptingOf(instance: WorkflowInstance<*>): Scripting =
    (instance as? ScriptedInstance<*>)?.scripting ?: Unscripted

/** What a journey that is not scripted plays: nothing, recording nothing. */
private object Unscripted : Scripting {
    override fun building(step: StepName, attempt: Attempt, found: Map<String, Captured>) = Unit

    override fun step(step: StepName, attempt: Attempt): Play<Outcome>? = null

    override fun rescue(rescue: StepName): Play<RescueOutcome>? = null
}
