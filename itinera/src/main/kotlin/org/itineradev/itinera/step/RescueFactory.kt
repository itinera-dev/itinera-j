package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync

/**
 * How a rescue step of a workflow of mode [M] is built: [SyncRescueFactory] for a synchronous
 * workflow, and `AsyncRescueFactory` from `itinera-kotlin` for an asynchronous one. A rescue step's
 * descriptor holds the factory of its own mode, so a rescue step of the other mode cannot join a
 * workflow; the executor checks it again before the journey.
 *
 * @param M the mode of the workflows the rescue step can join.
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
@ExperimentalItineraApi public interface RescueFactory<M : Mode>

/**
 * Builds a synchronous rescue step, as a class's constructor does: what it throws aborts the
 * journey with `step could not be built`. It reads what the rescue step requested from `got`, and
 * returns the run the executor then calls once.
 *
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
@ExperimentalItineraApi
public fun interface SyncRescueFactory : RescueFactory<Sync> {
    /**
     * Builds the rescue step, from the answers to the requests its descriptor declares.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public fun build(got: Got<RescueKind<Sync>>): SyncRescueRun
}

/**
 * The run of a synchronous rescue step, which returns its [RescueOutcome] once the work is done.
 * What it throws is an abnormal termination of the rescue step.
 *
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
@ExperimentalItineraApi
public fun interface SyncRescueRun {
    /**
     * Runs the rescue step.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public fun run(): RescueOutcome
}
