package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.step.samples.charging
import org.itineradev.itinera.step.samples.discounting
import org.itineradev.itinera.step.samples.factories
import org.itineradev.itinera.step.samples.outcomes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class OutcomeTest {
    @Test
    fun `a step's helper and the static factory make the same success`() {
        assertEquals(Outcome.success(), factories().build(Answers(emptyMap())).run())
    }

    @Test
    fun `the sample makes a success, failures and skips`() {
        val kinds = outcomes().map(::kindOf)
        assertEquals(
            listOf("Success", "Failure", "Failure", "Skipped", "Skipped", "Skipped"),
            kinds,
        )
    }

    @ParameterizedTest
    @FieldSource("helpers")
    fun `a step's helper makes the outcome the static factory makes`(
        made: Outcome,
        expected: Outcome,
    ) {
        assertEquals(expected, made)
    }

    @Test
    fun `a failure's details are captured when it is made`() {
        val details = mutableListOf("05")
        val failure = Outcome.failure(Reason("declined", null, details)) as Outcome.Failure
        details += "51"
        assertEquals(listOf("05"), failure.reason.details)
    }

    @Test
    fun `a skip's details are captured when it is made`() {
        val details = mutableMapOf("tier" to "gold")
        val skip = Outcome.skipped(Reason("no discount", null, details)) as Outcome.Skipped
        details["tier"] = "silver"
        assertEquals(mapOf("tier" to "gold"), skip.reason?.details)
    }

    @Test
    fun `details that are not a value are kept as given, for the engine to refuse`() {
        val thread = Thread()
        val failure = Outcome.failure(Reason("declined", null, thread)) as Outcome.Failure
        assertSame(thread, failure.reason.details)
    }

    private fun kindOf(outcome: Outcome): String? = outcome::class.simpleName

    companion object {
        private val step = object : ItineraStep {}

        @JvmField
        val helpers =
            listOf(
                argumentSet(
                    "a failure for a reason",
                    step.fail(Reason("declined", "No.")),
                    Outcome.failure(Reason("declined", "No.")),
                ),
                argumentSet(
                    "a failure for a code",
                    step.fail("declined"),
                    Outcome.failure(Reason("declined")),
                ),
                argumentSet("a skip with no reason", step.skip(), Outcome.skipped()),
                argumentSet(
                    "a skip for a reason",
                    step.skipBecause(Reason("none", "No.")),
                    Outcome.skipped(Reason("none", "No.")),
                ),
                argumentSet(
                    "a skip for a code",
                    step.skipBecause("none"),
                    Outcome.skipped(Reason("none")),
                ),
                argumentSet(
                    "a sample step that fails",
                    charging(),
                    Outcome.failure(
                        Reason("declined", "Nothing to charge.", mapOf("amount" to 0L))
                    ),
                ),
                argumentSet(
                    "a sample step that skips",
                    discounting(),
                    Outcome.skipped("no discount"),
                ),
            )
    }
}
