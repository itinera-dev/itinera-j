package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome

/**
 * How a test says the attempt of a [RescueStepMock] or an [AsyncRescueStepMock] ends, so that the
 * mock ends it exactly as a real rescue step would: it succeeds, fails, or terminates abnormally,
 * as an exception escaping a rescue step's run does. A rescue step never skips, so neither does a
 * rescue ending. A test that writes real rescue steps can end them the same way, through [outcome].
 *
 * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
 */
@ExperimentalItineraApi
public sealed interface RescueEnding {
    /**
     * The outcome a rescue step's run returns to end this way.
     *
     * @throws RuntimeException carrying the message, for an abnormal termination, as a rescue
     *   step's run throws it.
     * @sample org.itineradev.itinera.step.testing.samples.rescueOutcomes
     */
    public fun outcome(): RescueOutcome =
        when (this) {
            Success -> RescueOutcome.success()
            is Failure -> RescueOutcome.failure(reason)
            is AbnormalTermination -> throw ScriptedError(errorMessage)
        }

    /**
     * The attempt succeeds.
     *
     * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
     */
    public data object Success : RescueEnding

    /**
     * The attempt fails, for [reason].
     *
     * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
     */
    @JvmRecord
    public data class Failure(
        /** Why the attempt fails. */
        val reason: Reason
    ) : RescueEnding

    /**
     * The attempt terminates abnormally, with an exception whose message is [errorMessage].
     *
     * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
     */
    @JvmRecord
    public data class AbnormalTermination(
        /** The message of the exception that ends the attempt. */
        val errorMessage: String
    ) : RescueEnding

    /** Makes rescue endings. */
    public companion object {
        /**
         * The attempt succeeds.
         *
         * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
         */
        @JvmStatic public fun success(): RescueEnding = Success

        /**
         * The attempt fails, for [reason].
         *
         * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
         */
        @JvmStatic public fun failure(reason: Reason): RescueEnding = Failure(reason)

        /**
         * The attempt fails, for a reason holding only [code].
         *
         * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
         */
        @JvmStatic public fun failure(code: String): RescueEnding = Failure(Reason(code))

        /**
         * The attempt terminates abnormally, with an exception carrying the message.
         *
         * @sample org.itineradev.itinera.step.testing.samples.rescueEndings
         */
        @JvmStatic
        public fun abnormalTermination(errorMessage: String): RescueEnding =
            AbnormalTermination(errorMessage)
    }
}
