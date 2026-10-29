package org.itineradev.itinera.testing

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * A mistake in a test, such as a script that leaves out an attempt or a fake named after a provider
 * the workflow does not have. It is thrown where the mistake is made: while a fake provider or a
 * script is written, or when a scripted run is asked to run, before its journey starts, or where
 * the record of a scripted run is read. It is never a refusal, and no event is emitted. Its message
 * names each problem, and [problems] holds them.
 *
 * @sample org.itineradev.itinera.testing.samples.mistakes
 */
@ExperimentalItineraApi
public class TestMistake
internal constructor(
    /** Every problem found, in the order found. */
    public val problems: List<Mistake>
) : RuntimeException(messageOf(problems))

private fun messageOf(problems: List<Mistake>): String =
    "The test is mistaken: ${problems.joinToString("; ", transform = Mistake::description)}."
