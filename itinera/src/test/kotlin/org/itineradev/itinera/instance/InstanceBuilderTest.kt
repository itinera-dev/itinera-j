package org.itineradev.itinera.instance

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.samples.fromKotlin
import org.itineradev.itinera.instance.samples.instances
import org.itineradev.itinera.journey.JourneyId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class InstanceBuilderTest {
    @Test
    fun `an instance holds what it was created with`() {
        val instance = instances()
        assertEquals(JourneyId.of("corner-42"), instance.journeyId)
        assertEquals(mapOf("order" to "42", "price" to 40L, "discount" to 5L), instance.initialData)
        assertEquals(mapOf("tenant" to "acme"), instance.initialGlobalMetadata)
        assertEquals(1, instance.reporters.size)
    }

    @Test
    fun `an instance is claimed once`() {
        val instance = fromKotlin()
        assertTrue(instance.claim())
        assertFalse(instance.claim())
    }
}
