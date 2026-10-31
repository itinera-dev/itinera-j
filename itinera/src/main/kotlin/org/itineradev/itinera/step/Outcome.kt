package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * What an attempt of a step reports when its run returns. A step reports one with the methods of
 * [ItineraStep], such as [ItineraStep.succeed], and code outside a step class with the factories
 * below.
 *
 * @sample org.itineradev.itinera.step.samples.outcomes
 */
@ExperimentalItineraApi
public sealed interface Outcome {
    /**
     * The attempt succeeded: its contributions are committed, and the journey goes on to the next
     * step.
     *
     * @sample org.itineradev.itinera.step.samples.outcomes
     */
    public data object Success : Outcome

    /** Makes outcomes. */
    public companion object {
        /**
         * The outcome of an attempt that succeeded.
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun success(): Outcome = Success
    }
}
