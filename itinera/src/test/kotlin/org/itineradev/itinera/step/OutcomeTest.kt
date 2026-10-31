package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.step.samples.factories
import org.itineradev.itinera.step.samples.outcomes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class OutcomeTest {
    @Test
    fun `a step's helper and the static factory make the same success`() {
        assertEquals(outcomes(), factories().build(Answers(emptyMap())).run())
    }
}
