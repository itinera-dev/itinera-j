package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class JourneyIdTest {
    @Test
    fun `a journey ID is never empty`() {
        assertThrows<IllegalArgumentException> { JourneyId.of("") }
    }

    @Test
    fun `journey IDs spelling the same text are equal`() {
        assertEquals(JourneyId.of("order-42"), JourneyId.of("order-42"))
        assertEquals(JourneyId.of("order-42").hashCode(), JourneyId.of("order-42").hashCode())
    }

    @Test
    fun `a journey ID prints as its text`() {
        assertEquals("order-42", JourneyId.of("order-42").toString())
    }
}
