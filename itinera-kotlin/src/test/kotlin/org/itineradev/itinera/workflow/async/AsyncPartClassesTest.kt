package org.itineradev.itinera.workflow.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.async.samples.asyncDescribingParts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncPartClassesTest {
    /** What a journey of the asynchronous shop renewed, charged and audited. */
    data class Audited(val card: String, val charged: Long, val audit: String)

    @Test
    fun `asynchronous parts described alone run in a workflow declared by hand`() =
        runBlocking<Unit> {
            val events = mutableListOf<Event>()
            val result = asyncDescribingParts(events::add)
            assertEquals(
                Audited("4242 renewed", 40L, "charged 40"),
                result.output(TypeToken.of(Audited::class.java)),
            )
            assertEquals(
                listOf("paid 40"),
                events.filterIsInstance<JourneyInfo>().map(JourneyInfo::message),
            )
        }
}
