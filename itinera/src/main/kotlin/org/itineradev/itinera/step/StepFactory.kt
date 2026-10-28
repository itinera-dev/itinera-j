package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync

/**
 * How a step of a workflow of mode [M] is built for each attempt: [SyncStepFactory] for a
 * synchronous workflow, and `AsyncStepFactory` from `itinera-kotlin` for an asynchronous one. A
 * step descriptor holds the factory of its own mode, so a step of the other mode cannot join a
 * workflow; the executor checks it again before the journey.
 *
 * @param M the mode of the workflows the step can join.
 * @sample org.itineradev.itinera.step.samples.factories
 */
@ExperimentalItineraApi public interface StepFactory<M : Mode>

/**
 * Builds a synchronous step for one attempt, as a class's constructor does: what it throws aborts
 * the journey with `step could not be built`. It returns the run the executor then calls once.
 *
 * @sample org.itineradev.itinera.step.samples.factories
 */
@ExperimentalItineraApi
public fun interface SyncStepFactory : StepFactory<Sync> {
    /**
     * Builds the step for one attempt.
     *
     * @sample org.itineradev.itinera.step.samples.factories
     */
    public fun build(): SyncStepRun
}

/**
 * One attempt's run of a synchronous step, which returns its [Outcome] once the work is done. What
 * it throws is an abnormal termination of the attempt.
 *
 * @sample org.itineradev.itinera.step.samples.factories
 */
@ExperimentalItineraApi
public fun interface SyncStepRun {
    /**
     * Runs the attempt.
     *
     * @sample org.itineradev.itinera.step.samples.factories
     */
    public fun run(): Outcome
}
