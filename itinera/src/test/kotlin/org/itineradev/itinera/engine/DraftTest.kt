package org.itineradev.itinera.engine

import java.util.UUID
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.packing
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.JourneyIdGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class DraftTest {
    @Test
    fun `without an ID generator each journey ID is a UUID version 4`() {
        val id = packing().create().journeyId.toString()
        assertEquals(4, UUID.fromString(id).version())
    }

    @Test
    fun `the ID generator reads the workflow object and the captured initial data`() {
        val instance = withIds { shop, data ->
            "${shop.javaClass.simpleName}-${data.read("order", TypeToken.of(String::class.java)).orNull()}"
        }
            .data("order", "42")
            .create()
        assertEquals(JourneyId.of("Shop-42"), instance.journeyId)
    }

    @Test
    fun `an ID generator that throws makes create throw, carrying the error`() {
        val error = IllegalStateException("no counter")
        val thrown =
            assertThrows<IllegalStateException> { withIds { _, _ -> throw error }.create() }
        assertSame(error, thrown.cause)
    }

    @Test
    fun `an empty journey ID makes create throw`() {
        assertThrows<IllegalStateException> { withIds { _, _ -> "" }.create() }
    }

    @Test
    fun `each reporter factory is given the journey ID, after it is produced`() {
        val given = mutableListOf<JourneyId>()
        val instance = withIds { _, _ ->
            "order-42"
        }
            .reporterFor { journeyId -> Reporter {}.also { given += journeyId } }
            .create()
        assertEquals(listOf(instance.journeyId), given)
    }

    @Test
    fun `a reporter factory that throws makes create throw, carrying the error`() {
        val error = IllegalArgumentException("no printer")
        val thrown =
            assertThrows<IllegalStateException> {
                packing().reporterFor { throw error }.create()
            }
        assertSame(error, thrown.cause)
    }

    @Test
    fun `initial data put twice under one key keeps the later value, in the first one's place`() {
        val instance = packing().data("a", 1L).initialData(mapOf("b" to 2L, "a" to 3L)).create()
        assertEquals(mapOf("a" to 3L, "b" to 2L), instance.initialData)
        assertEquals(listOf("a", "b"), instance.initialData.keys.toList())
    }

    private fun withIds(generator: JourneyIdGenerator<Shop>) =
        instanceOf(shop().step(succeeding("pack").build()).idGenerator(generator).build())
}
