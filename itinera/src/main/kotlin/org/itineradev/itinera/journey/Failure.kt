package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.step.Reason

/**
 * Why a journey failed: the [details] its `journey_failed` event carries, with the error that ended
 * the step, when one did.
 *
 * @sample org.itineradev.itinera.journey.samples.results
 */
@ExperimentalItineraApi
@JvmRecord
public data class Failure(
    /** The cause, reason and error message, as `journey_failed` carries them. */
    val details: JourneyFailure,
    /** What the step threw, when an abnormal termination failed the journey. */
    val error: Throwable?,
) {
    /** Why the journey failed. */
    val cause: FailureCause
        get() = details.cause

    /** The reason the step failed with, when it failed with one. */
    val reason: Reason?
        get() = details.reason

    /** The message of the error that ended the step, when one did. */
    val errorMessage: String?
        get() = details.errorMessage
}
