package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * How a journey ended, as `run` returns it once the journey is over: its journey ID and its
 * [status]. Nothing that happens inside a journey escapes `run`, so every journey ends in one.
 *
 * [isSucceeded], [isFailed], [isAborted], [failure] and [abort] read it without a `when` or a
 * `switch`.
 *
 * @sample org.itineradev.itinera.journey.samples.results
 */
@ExperimentalItineraApi
@JvmRecord
public data class JourneyResult(
    /** The journey's ID, which every event of the journey carries. */
    val journeyId: JourneyId,
    /** Whether the journey succeeded, failed or was aborted, with what each carries. */
    val status: JourneyStatus,
) {
    /**
     * Whether the journey succeeded.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    val isSucceeded: Boolean
        get() = status is JourneyStatus.Succeeded

    /**
     * Whether the journey failed.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    val isFailed: Boolean
        get() = status is JourneyStatus.Failed

    /**
     * Whether the journey was aborted.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    val isAborted: Boolean
        get() = status is JourneyStatus.Aborted

    /**
     * Why the journey failed, when it failed.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    val failure: Failure?
        get() = (status as? JourneyStatus.Failed)?.failure

    /**
     * Why the journey was aborted, when it was.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    val abort: Abort?
        get() = (status as? JourneyStatus.Aborted)?.abort
}

/**
 * How a journey ended: [Succeeded] or [Failed], each with its data bag as the journey left it, or
 * [Aborted], with no data bag.
 *
 * @sample org.itineradev.itinera.journey.samples.results
 */
@ExperimentalItineraApi
public sealed interface JourneyStatus {
    /**
     * Every step ended without failing the journey.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    @JvmRecord
    public data class Succeeded(
        /** The data bag as the journey left it. */
        val dataBag: DataBag
    ) : JourneyStatus

    /**
     * A step failed the journey.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    @JvmRecord
    public data class Failed(
        /** Why the journey failed. */
        val failure: Failure,
        /** The data bag as the journey left it. */
        val dataBag: DataBag,
    ) : JourneyStatus

    /**
     * Something went wrong that the workflow cannot decide on, such as a step that could not be
     * built or a reporter that failed.
     *
     * @sample org.itineradev.itinera.journey.samples.results
     */
    @JvmRecord
    public data class Aborted(
        /** Why the journey was aborted. */
        val abort: Abort
    ) : JourneyStatus
}
