package org.itineradev.itinera.step.async

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.workflow.Async

/**
 * Builds an asynchronous step for one attempt, as a class's constructor does: what it throws aborts
 * the journey with `step could not be built`. It reads what the step requested from `got`, and
 * returns the run the executor then awaits once.
 *
 * @sample org.itineradev.itinera.step.async.samples.asyncFactories
 */
public fun interface AsyncStepFactory : StepFactory<Async> {
    /**
     * Builds the step for one attempt, from the answers to the requests its descriptor declares.
     *
     * @sample org.itineradev.itinera.step.async.samples.asyncFactories
     */
    public fun build(got: Got<StepKind<Async>>): AsyncStepRun
}

/**
 * One attempt's run of an asynchronous step, which returns its [Outcome] once the work is done.
 * What it throws is an abnormal termination of the attempt, a `CancellationException` of its own,
 * such as a `withTimeout` that expired, included.
 *
 * @sample org.itineradev.itinera.step.async.samples.asyncFactories
 */
public fun interface AsyncStepRun {
    /**
     * Runs the attempt.
     *
     * @sample org.itineradev.itinera.step.async.samples.asyncFactories
     */
    @JvmSynthetic public suspend fun run(): Outcome
}
