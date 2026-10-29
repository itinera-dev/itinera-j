package org.itineradev.itinera.testing

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * The kinds of mistake in a test, each written in words, such as `attempt left out`, by its
 * [toString].
 *
 * @sample org.itineradev.itinera.testing.samples.mistakes
 */
@ExperimentalItineraApi
public enum class MistakeKind {
    /** [Mistake.UnknownStep]. */
    UNKNOWN_STEP,

    /** [Mistake.StepScriptedTwice]. */
    STEP_SCRIPTED_TWICE,

    /** [Mistake.GapInTheAttempts]. */
    GAP_IN_THE_ATTEMPTS,

    /** [Mistake.AttemptScriptedTwice]. */
    ATTEMPT_SCRIPTED_TWICE,

    /** [Mistake.NoSuchAttempt]. */
    NO_SUCH_ATTEMPT,

    /** [Mistake.RescueStepSkips]. */
    RESCUE_STEP_SKIPS,

    /** [Mistake.NoContributor]. */
    NO_CONTRIBUTOR,

    /** [Mistake.NoReporter]. */
    NO_REPORTER,

    /** [Mistake.AttemptLeftOut]. */
    ATTEMPT_LEFT_OUT,

    /** [Mistake.RescueStepLeftOut]. */
    RESCUE_STEP_LEFT_OUT,

    /** [Mistake.StepLeftOut]. */
    STEP_LEFT_OUT,

    /** [Mistake.UnknownProvider]. */
    UNKNOWN_PROVIDER,

    /** [Mistake.AnotherService]. */
    ANOTHER_SERVICE;

    /** The kind in words, such as `attempt left out`. */
    override fun toString(): String = name.lowercase().replace('_', ' ')
}
