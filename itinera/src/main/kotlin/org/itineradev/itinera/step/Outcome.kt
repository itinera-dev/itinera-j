package org.itineradev.itinera.step

import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.NotAValue
import org.itineradev.itinera.engine.Values

/**
 * What an attempt of a step reports when its run returns. A step reports one with the methods of
 * [ItineraStep], such as [ItineraStep.succeed], and code outside a step class with the factories
 * below.
 *
 * A reason's details are copied when the outcome is made, so changing them afterwards changes
 * nothing the outcome holds, and copied again once the attempt returns the outcome, so nothing the
 * journey holds can change. Details that are not a value abort the journey with `not a value` once
 * the attempt returns the outcome.
 *
 * @sample org.itineradev.itinera.step.samples.outcomes
 */
public sealed interface Outcome {
    /**
     * The attempt succeeded: its contributions are committed, and the journey goes on to the next
     * step.
     *
     * @sample org.itineradev.itinera.step.samples.outcomes
     */
    public data object Success : Outcome

    /**
     * The attempt could not complete, for [reason]: its contributions are never committed, and the
     * step is attempted again while its retry budget lasts.
     *
     * @sample org.itineradev.itinera.step.samples.outcomes
     */
    @ConsistentCopyVisibility
    public data class Failure
    internal constructor(
        /** Why the step failed, with the copy of its details made when the outcome was made. */
        val reason: Reason
    ) : Outcome

    /**
     * The step decided not to do its work, for [reason] when it gave one: its contributions are
     * discarded, it never runs again in the journey, and the journey goes on to the next step.
     *
     * @sample org.itineradev.itinera.step.samples.outcomes
     */
    @ConsistentCopyVisibility
    public data class Skipped
    internal constructor(
        /**
         * Why the step was skipped, with the copy of its details made when the outcome was made, or
         * `null` when it gave no reason.
         */
        val reason: Reason?
    ) : Outcome

    /** Makes outcomes. */
    public companion object {
        /**
         * The outcome of an attempt that succeeded.
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun success(): Outcome = Success

        /**
         * The outcome of an attempt that could not complete, for [reason].
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun failure(reason: Reason): Outcome = Failure(captured(reason))

        /**
         * The outcome of an attempt that could not complete, for a reason holding only [code].
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun failure(code: String): Outcome = Failure(Reason(code))

        /**
         * The outcome of a step that decided not to do its work, giving no reason.
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun skipped(): Outcome = Skipped(null)

        /**
         * The outcome of a step that decided not to do its work, for [reason].
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun skipped(reason: Reason): Outcome = Skipped(captured(reason))

        /**
         * The outcome of a step that decided not to do its work, for a reason holding only [code].
         *
         * @sample org.itineradev.itinera.step.samples.outcomes
         */
        @JvmStatic public fun skipped(code: String): Outcome = Skipped(Reason(code))
    }
}

/**
 * [reason] with a copy of its details, or [reason] itself when its details are not a value, which
 * the engine finds again once the attempt returns.
 */
private fun captured(reason: Reason): Reason =
    when (val capture = Values.capture(reason.details)) {
        is Captured -> reason.copy(details = capture.value)
        is NotAValue -> reason
    }
