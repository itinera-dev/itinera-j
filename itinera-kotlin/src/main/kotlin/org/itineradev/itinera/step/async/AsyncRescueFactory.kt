package org.itineradev.itinera.step.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.RescueFactory
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.workflow.Async

/**
 * Builds an asynchronous rescue step, as a class's constructor does: what it throws aborts the
 * journey with `step could not be built`. It reads what the rescue step requested from `got`, and
 * returns the run the executor then awaits once.
 *
 * @sample org.itineradev.itinera.workflow.async.samples.asyncRescues
 */
@ExperimentalItineraApi
public fun interface AsyncRescueFactory : RescueFactory<Async> {
    /**
     * Builds the rescue step, from the answers to the requests its descriptor declares.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncRescues
     */
    public fun build(got: Got<RescueKind<Async>>): AsyncRescueRun
}

/**
 * The run of an asynchronous rescue step, which returns its [RescueOutcome] once the work is done.
 * What it throws is an abnormal termination of the rescue step, a `CancellationException` of its
 * own, such as a `withTimeout` that expired, included.
 *
 * @sample org.itineradev.itinera.workflow.async.samples.asyncRescues
 */
@ExperimentalItineraApi
public fun interface AsyncRescueRun {
    /**
     * Runs the rescue step.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncRescues
     */
    @JvmSynthetic public suspend fun run(): RescueOutcome
}
