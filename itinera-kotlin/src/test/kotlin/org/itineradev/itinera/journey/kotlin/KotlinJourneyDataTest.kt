package org.itineradev.itinera.journey.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.Datum
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.journey.OutputException
import org.itineradev.itinera.journey.OutputProblem
import org.itineradev.itinera.journey.kotlin.samples.outputs
import org.itineradev.itinera.value.kotlin.bagOf
import org.itineradev.itinera.value.kotlin.bagWith
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.itineradev.itinera.workflow.async.samples.asyncBuilding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

data class Box<T>(val content: T)

data class Note(@Datum(required = true) val note: String?)

@OptIn(ExperimentalItineraApi::class)
class KotlinJourneyDataTest {
    private fun succeeded(bag: DataBag): JourneyResult =
        JourneyResult(JourneyId.of("order-42"), JourneyStatus.Succeeded(bag))

    @Test
    fun `a nullable component reads null when its key holds nothing`() {
        val bag = bagWith("order" to "42", "amount" to 40L)
        assertEquals("order 42 charged 40, no note", outputs(succeeded(bag)))
        val noted = bagWith("order" to "42", "amount" to 40L, "note" to "fragile")
        assertEquals("order 42 charged 40, fragile", outputs(succeeded(noted)))
    }

    @Test
    fun `a component that is not nullable does not read null`() {
        val unread =
            assertThrows<OutputException> { succeeded(bagOf("content", null)).output<Box<Long>>() }
        assertEquals(OutputProblem.OtherType("content"), unread.problem)
    }

    @Test
    fun `a generic data class reads each component as the type its arguments give`() {
        assertEquals(Box(40L), succeeded(bagOf("content", 40L)).output<Box<Long>>())
        val unread =
            assertThrows<OutputException> { succeeded(bagOf("content", "40")).output<Box<Long>>() }
        assertEquals(OutputProblem.OtherType("content"), unread.problem)
    }

    @Test
    fun `a nullable component marked required needs its key, which may hold null`() {
        assertEquals(Note(null), succeeded(bagOf("note", null)).output<Note>())
        val unread = assertThrows<OutputException> { succeeded(bagWith()).output<Note>() }
        assertEquals(OutputProblem.Absent("note"), unread.problem)
    }

    @Test
    fun `a data class gives a nullable component marked required as initial data though it holds null`() {
        val instance =
            asyncBuilding().instance(AsyncOrders("corner")).initialData(Note(null)).create()
        assertEquals(mapOf("note" to null), instance.initialData)
    }
}
