package org.itineradev.itinera.journey

/**
 * Thrown when a journey's output cannot be read as the record or data class asked for, as [problem]
 * says.
 *
 * @sample org.itineradev.itinera.journey.samples.outputs
 */
public class OutputException(
    /** Why the output could not be read. */
    public val problem: OutputProblem
) : RuntimeException(messageOf(problem))

/**
 * Why a journey's output could not be read: the journey was [Aborted], so it has no data bag, or a
 * required key is [Absent] or holds a value of [OtherType].
 *
 * Kotlin matches it with `when`, and Java with `switch` or `instanceof`.
 *
 * @sample org.itineradev.itinera.journey.samples.outputs
 */
public sealed interface OutputProblem {
    /**
     * The journey was aborted, so it has no data bag to read.
     *
     * @sample org.itineradev.itinera.journey.samples.outputs
     */
    public data object Aborted : OutputProblem

    /**
     * The data bag holds nothing under a key that a component requires.
     *
     * @sample org.itineradev.itinera.journey.samples.outputs
     */
    @JvmRecord
    public data class Absent(
        /** The key. */
        val key: String
    ) : OutputProblem

    /**
     * The data bag holds a value under a key that is not of its component's type.
     *
     * @sample org.itineradev.itinera.journey.samples.outputs
     */
    @JvmRecord
    public data class OtherType(
        /** The key. */
        val key: String
    ) : OutputProblem
}

private fun messageOf(problem: OutputProblem): String =
    when (problem) {
        OutputProblem.Aborted -> "The journey was aborted, so it has no data bag to read."
        is OutputProblem.Absent -> "The data bag holds no value under ${problem.key}."
        is OutputProblem.OtherType ->
            "The value under ${problem.key} is not of the type its component reads."
    }
