package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.JourneyData
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.value.TypeToken

/**
 * How a journey ended, as `run` returns it once the journey is over: its journey ID and its
 * [status]. Nothing that happens inside a journey escapes `run`, so every journey ends in one.
 *
 * [isSucceeded], [isFailed], [isAborted], [failure] and [abort] read it without a `when` or a
 * `switch`, and [output] reads the data bag as a record or data class.
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

    /**
     * Reads the data bag of a journey that succeeded or failed as the record or data class [type]
     * names, each component the reader's own copy of the value under its key: its name, unless
     * [Datum] names another. An optional component, as [Datum] says, takes `null`, or an empty
     * `Optional`, when its key holds nothing. Keys that no component names are left unread.
     *
     * Building the record or data class calls its constructor, and what it throws escapes.
     *
     * @throws OutputException when the journey was aborted, or on the first key that holds nothing
     *   a required component needs, or a value of another type than its component's.
     * @throws IllegalArgumentException when [type] is not a record or a data class, or a Kotlin
     *   data class without `itinera-kotlin`; when two of its components give one key; when it marks
     *   an `Optional` component required, or has one without `itinera-java`; or when no value can
     *   be of a component's type.
     * @sample org.itineradev.itinera.journey.samples.outputs
     */
    public fun <T> output(type: TypeToken<T>): T {
        val owner = type.type
        require(owner is ClassType) {
            "The output is read as a record or a data class, not $owner."
        }
        val bag = dataBag() ?: throw OutputException(OutputProblem.Aborted)
        return returned(JourneyData.of(owner).read(bag))
    }

    private fun dataBag(): DataBag? =
        when (status) {
            is JourneyStatus.Succeeded -> status.dataBag
            is JourneyStatus.Failed -> status.dataBag
            is JourneyStatus.Aborted -> null
        }
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
