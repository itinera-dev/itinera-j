package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.DataBags
import org.itineradev.itinera.value.Fragile
import org.itineradev.itinera.value.Parcel
import org.itineradev.itinera.value.Rebuilds
import org.itineradev.itinera.value.Ticket
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class ValuesTest {
    @AfterEach
    fun rebuildAgain() {
        Rebuilds.fail = false
    }

    @Test
    fun `a copy that throws while a value is captured makes it not a value, carrying the error`() {
        val refused = Values.capture(setOf(Fragile("vase"))) as NotAValue
        assertEquals("cannot hash vase", refused.error?.message)
    }

    @Test
    fun `a constructor that throws while a value is captured makes it not a value, carrying the error`() {
        val ticket = Ticket("A1")
        Rebuilds.fail = true
        val refused = Values.capture(ticket) as NotAValue
        assertEquals("no more tickets", refused.error?.message)
    }

    @Test
    fun `what is not a value carries no error`() {
        assertNull((Values.capture(Thread.currentThread()) as NotAValue).error)
    }

    @Test
    fun `a copy that throws while a value is read makes it of the wrong type, carrying the error`() {
        val stored = DataBags.captured(Ticket("A1"))
        Rebuilds.fail = true
        val wrong = Values.receive(stored, TypeToken.of(Ticket::class.java)) as WrongType
        assertEquals("no more tickets", wrong.error?.message)
    }

    @Test
    fun `a value that is not of the type read carries no error`() {
        val wrong =
            Values.receive(DataBags.captured(40), TypeToken.of(Long::class.java)) as WrongType
        assertNull(wrong.error)
    }

    @Test
    fun `each receiver gets a copy of its own`() {
        val stored = DataBags.captured(Parcel(1200L, mutableListOf("book")))
        received(stored).items += "lamp"
        assertEquals(listOf("book"), received(stored).items)
        assertEquals(listOf("book"), (stored.value as Parcel).items)
    }

    private fun received(stored: Captured): Parcel =
        (Values.receive(stored, TypeToken.of(Parcel::class.java)) as Received).value
}
