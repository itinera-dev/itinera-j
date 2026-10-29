package org.itineradev.itinera.step

/**
 * What a rescue step's run reports when it returns: a success or a failure. It has no skip, since a
 * rescue step either fixes the cause of its step's failure or fails, so a rescue step that skips
 * cannot be written. A rescue step reports one with the methods of [ItineraRescueStep], such as
 * [ItineraRescueStep.succeed], and code outside a rescue step class with the factories below.
 *
 * A reason's details are copied when the outcome is made, and again once the rescue step returns
 * the outcome, as for a step's [Outcome].
 *
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
public sealed interface RescueOutcome {
    /**
     * The rescue step succeeded: its contributions are committed, and the step it rescues is
     * attempted once more.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public data object Success : RescueOutcome

    /**
     * The rescue step could not fix the cause, for [reason]: its contributions are never committed,
     * the step it rescues never runs again, and the journey fails.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    @ConsistentCopyVisibility
    public data class Failure
    internal constructor(
        /**
         * Why the rescue step failed, with the copy of its details made when the outcome was made.
         */
        val reason: Reason
    ) : RescueOutcome

    /** Makes the outcomes of rescue steps. */
    public companion object {
        /**
         * The outcome of a rescue step that succeeded.
         *
         * @sample org.itineradev.itinera.workflow.samples.rescues
         */
        @JvmStatic public fun success(): RescueOutcome = Success

        /**
         * The outcome of a rescue step that could not fix the cause, for [reason].
         *
         * @sample org.itineradev.itinera.workflow.samples.rescues
         */
        @JvmStatic public fun failure(reason: Reason): RescueOutcome = Failure(captured(reason))

        /**
         * The outcome of a rescue step that could not fix the cause, for a reason holding only
         * [code].
         *
         * @sample org.itineradev.itinera.workflow.samples.rescues
         */
        @JvmStatic public fun failure(code: String): RescueOutcome = Failure(Reason(code))
    }
}
