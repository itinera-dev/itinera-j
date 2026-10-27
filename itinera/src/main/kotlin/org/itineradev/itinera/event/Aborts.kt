package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.step.StepName

/**
 * A step hook returned a lifecycle it may not return: a failure hook finished the workflow.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
@JvmRecord
public data class InvalidLifecycle(override val step: StepName) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.INVALID_LIFECYCLE

    override val errorMessage: String?
        get() = null
}

/**
 * A hook, or a role operation it called, failed.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
@JvmRecord
public data class HookFailed(override val step: StepName?, override val errorMessage: String) :
    JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.HOOK_FAILED
}

/**
 * A reporter, or a dispatcher while dispatching, failed.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
@JvmRecord
public data class ReporterFailed(override val step: StepName?, override val errorMessage: String) :
    JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.REPORTER_FAILED
}

/**
 * A rescue step reported Skipped.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
@JvmRecord
public data class RescueSkipped(override val step: StepName) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.RESCUE_SKIPPED

    override val errorMessage: String?
        get() = null
}

/**
 * A step, a hook or a service provider set global metadata under `journey_id`, the journey ID's own
 * key, which only the executor sets.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
@JvmRecord
public data class ReservedMetadataKey(
    override val step: StepName?,
    /** Who set it. */
    val source: ValueSource,
) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.RESERVED_METADATA_KEY

    override val errorMessage: String?
        get() = null

    /** The key it was set under, the only one reserved. */
    val key: String
        get() = "journey_id"
}

/**
 * Custom code returned something its call cannot return, such as `null` where a result is required,
 * which only Java can write. It is not a failure of that code, so it carries no error, and it is
 * never retried.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
@JvmRecord
public data class UnexpectedResult(
    override val step: StepName,
    /** What returned it. */
    val source: ResultSource,
    /** What was returned, and by whom, for the people reading it. */
    val message: String,
) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.UNEXPECTED_RESULT

    override val errorMessage: String?
        get() = null
}
