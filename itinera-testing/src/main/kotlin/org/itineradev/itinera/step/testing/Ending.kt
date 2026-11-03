package org.itineradev.itinera.step.testing

import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason

/**
 * How a test says an attempt of a [StepMock] or an [AsyncStepMock] ends, so that the mock ends it
 * exactly as a real step would: it succeeds, fails, is skipped, or terminates abnormally, as an
 * exception escaping a step's run does. A test that writes real steps can end them the same way,
 * through [outcome].
 *
 * @sample org.itineradev.itinera.step.testing.samples.endings
 */
public sealed interface Ending {
    /**
     * The outcome a step's run returns to end this way.
     *
     * @throws RuntimeException carrying the message, for an abnormal termination, as a step's run
     *   throws it.
     * @sample org.itineradev.itinera.step.testing.samples.outcomes
     */
    public fun outcome(): Outcome =
        when (this) {
            Success -> Outcome.success()
            is Failure -> Outcome.failure(reason)
            is Skipped -> reason?.let(Outcome::skipped) ?: Outcome.skipped()
            is AbnormalTermination -> throw ScriptedError(errorMessage)
        }

    /**
     * The attempt succeeds.
     *
     * @sample org.itineradev.itinera.step.testing.samples.endings
     */
    public data object Success : Ending

    /**
     * The attempt fails, for [reason].
     *
     * @sample org.itineradev.itinera.step.testing.samples.endings
     */
    @JvmRecord
    public data class Failure(
        /** Why the attempt fails. */
        val reason: Reason
    ) : Ending

    /**
     * The attempt is skipped, for [reason] when it has one.
     *
     * @sample org.itineradev.itinera.step.testing.samples.endings
     */
    @JvmRecord
    public data class Skipped(
        /** Why the attempt is skipped, or `null` for no reason. */
        val reason: Reason?
    ) : Ending

    /**
     * The attempt terminates abnormally, with an exception whose message is [errorMessage].
     *
     * @sample org.itineradev.itinera.step.testing.samples.endings
     */
    @JvmRecord
    public data class AbnormalTermination(
        /** The message of the exception that ends the attempt. */
        val errorMessage: String
    ) : Ending

    /** Makes endings. */
    public companion object {
        /**
         * The attempt succeeds.
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic public fun success(): Ending = Success

        /**
         * The attempt fails, for [reason].
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic public fun failure(reason: Reason): Ending = Failure(reason)

        /**
         * The attempt fails, for a reason holding only [code].
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic public fun failure(code: String): Ending = Failure(Reason(code))

        /**
         * The attempt is skipped, for no reason.
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic public fun skipped(): Ending = Skipped(null)

        /**
         * The attempt is skipped, for [reason].
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic public fun skipped(reason: Reason): Ending = Skipped(reason)

        /**
         * The attempt is skipped, for a reason holding only [code].
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic public fun skipped(code: String): Ending = Skipped(Reason(code))

        /**
         * The attempt terminates abnormally, with an exception carrying the message.
         *
         * @sample org.itineradev.itinera.step.testing.samples.endings
         */
        @JvmStatic
        public fun abnormalTermination(errorMessage: String): Ending =
            AbnormalTermination(errorMessage)
    }
}

/**
 * What a step mock or a rescue step mock throws to end an attempt abnormally, or to fail its build.
 */
internal class ScriptedError(message: String) : RuntimeException(message)
