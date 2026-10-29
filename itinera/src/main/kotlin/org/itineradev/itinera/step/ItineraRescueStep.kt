package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * A rescue step written as a class: what fixes the cause of a step's failure, after which the step
 * runs once more. A workflow gives it to the step it rescues with `@WithStep(rescue = ...)`. The
 * class has one method named `run`, which returns a [RescueOutcome], never a step's [Outcome], so a
 * rescue step cannot skip: an ordinary method in a synchronous workflow, and a `suspend` one in an
 * asynchronous workflow. The executor builds the class with its constructor each time it rescues
 * its step, then calls `run` once.
 *
 * The interface's methods make the outcomes `run` returns.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.renewing
 */
@ExperimentalItineraApi
public interface ItineraRescueStep {
    /**
     * The outcome of a rescue that succeeded.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.renewing
     */
    public fun succeed(): RescueOutcome = RescueOutcome.success()

    /**
     * The outcome of a rescue that could not fix the cause, for [reason].
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.renewing
     */
    public fun fail(reason: Reason): RescueOutcome = RescueOutcome.failure(reason)

    /**
     * The outcome of a rescue that could not fix the cause, for a reason holding only [code].
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.renewing
     */
    public fun fail(code: String): RescueOutcome = RescueOutcome.failure(code)
}
