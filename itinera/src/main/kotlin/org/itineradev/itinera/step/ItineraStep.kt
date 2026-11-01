package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * A step written as a class: one unit of a workflow's business logic, which a workflow names among
 * its steps. The class has one method named `run`, which returns an [Outcome]: an ordinary method
 * in a synchronous workflow, and a `suspend` one in an asynchronous workflow. The executor builds
 * the class anew for every attempt, with its constructor, then calls `run` once; anything either
 * throws ends the attempt, and the step never keeps anything from one attempt to the next.
 *
 * The interface's methods make the outcomes `run` returns: it succeeds, fails or skips.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@ExperimentalItineraApi
public interface ItineraStep {
    /**
     * The outcome of an attempt that succeeded.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
     */
    public fun succeed(): Outcome = Outcome.success()

    /**
     * The outcome of an attempt that could not complete, for [reason].
     *
     * @sample org.itineradev.itinera.step.samples.charging
     */
    public fun fail(reason: Reason): Outcome = Outcome.failure(reason)

    /**
     * The outcome of an attempt that could not complete, for a reason holding only [code].
     *
     * @sample org.itineradev.itinera.step.samples.charging
     */
    public fun fail(code: String): Outcome = Outcome.failure(code)

    /**
     * The outcome of a step that decided not to do its work, giving no reason.
     *
     * @sample org.itineradev.itinera.step.samples.discounting
     */
    public fun skip(): Outcome = Outcome.skipped()

    /**
     * The outcome of a step that decided not to do its work, for [reason].
     *
     * @sample org.itineradev.itinera.step.samples.discounting
     */
    public fun skipBecause(reason: Reason): Outcome = Outcome.skipped(reason)

    /**
     * The outcome of a step that decided not to do its work, for a reason holding only [code].
     *
     * @sample org.itineradev.itinera.step.samples.discounting
     */
    public fun skipBecause(code: String): Outcome = Outcome.skipped(code)
}
