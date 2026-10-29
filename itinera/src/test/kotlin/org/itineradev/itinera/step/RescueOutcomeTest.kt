package org.itineradev.itinera.step

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class RescueOutcomeTest {
    @ParameterizedTest
    @FieldSource("helpers")
    fun `a rescue step's helper makes the outcome the static factory makes`(
        made: RescueOutcome,
        expected: RescueOutcome,
    ) {
        assertEquals(expected, made)
    }

    @Test
    fun `a rescue step's failure's details are captured when it is made`() {
        val details = mutableListOf("05")
        val failure =
            RescueOutcome.failure(Reason("stolen", null, details)) as RescueOutcome.Failure
        details += "51"
        assertEquals(listOf("05"), failure.reason.details)
    }

    companion object {
        private val rescue = object : ItineraRescueStep {}

        @JvmField
        val helpers =
            listOf(
                argumentSet("a success", rescue.succeed(), RescueOutcome.success()),
                argumentSet(
                    "a failure for a reason",
                    rescue.fail(Reason("stolen", "No.")),
                    RescueOutcome.failure(Reason("stolen", "No.")),
                ),
                argumentSet(
                    "a failure for a code",
                    rescue.fail("stolen"),
                    RescueOutcome.failure(Reason("stolen")),
                ),
            )
    }
}
