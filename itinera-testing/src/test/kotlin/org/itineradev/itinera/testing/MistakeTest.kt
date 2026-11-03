@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.testing.samples.mistakes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MistakeTest {
    @Test
    fun `a test mistake names the problem found`() {
        assertEquals(listOf(Mistake.UnknownProvider("courier")), mistakes())
    }

    @Test
    fun `a test mistake's message names every problem, in the order found`() {
        val mistake =
            TestMistake(listOf(Mistake.AttemptLeftOut("charge", 2), Mistake.StepLeftOut("ship")))
        assertEquals(
            "The test is mistaken: attempt 2 of charge is left out; " +
                "ship is left out, though every step must be scripted.",
            mistake.message,
        )
    }

    @Test
    fun `a kind of mistake reads in words`() {
        assertEquals("attempt left out", MistakeKind.ATTEMPT_LEFT_OUT.toString())
    }
}
