package org.itineradev.itinera.journey

import org.itineradev.itinera.event.JourneyAbort
import org.itineradev.itinera.step.StepName

/**
 * Why a journey was aborted: the [details] its `journey_aborted` event carries, with the error that
 * aborted it, when one did.
 *
 * @sample org.itineradev.itinera.journey.samples.results
 */
@JvmRecord
public data class Abort(
    /** The abort reason and what it carries, as `journey_aborted` carries them. */
    val details: JourneyAbort,
    /** What the custom code threw, when the abort comes from an error. */
    val error: Throwable?,
) {
    /** The abort reason. */
    val reason: AbortReason
        get() = details.reason

    /** The step concerned, if any. */
    val step: StepName?
        get() = details.step

    /** The message of the error that aborted the journey, when one did. */
    val errorMessage: String?
        get() = details.errorMessage
}
