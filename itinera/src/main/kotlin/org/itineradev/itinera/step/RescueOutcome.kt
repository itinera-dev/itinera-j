package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * What a rescue step's run reports when it returns. It has no skip, since a rescue step either
 * fixes the cause of its step's failure or fails, so a rescue step that skips cannot be written.
 *
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
@ExperimentalItineraApi
public sealed interface RescueOutcome {
    /**
     * The rescue step succeeded: its contributions are committed, and the step it rescues is
     * attempted once more.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public data object Success : RescueOutcome

    /** Makes the outcomes of rescue steps. */
    public companion object {
        /**
         * The outcome of a rescue step that succeeded.
         *
         * @sample org.itineradev.itinera.workflow.samples.rescues
         */
        @JvmStatic public fun success(): RescueOutcome = Success
    }
}
