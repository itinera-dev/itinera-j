package org.itineradev.itinera.journey.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.kotlin.samples.Parcel
import org.itineradev.itinera.journey.kotlin.samples.reads
import org.itineradev.itinera.value.kotlin.bagOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class ReadsTest {
    @Test
    fun `a reified read gives the reader's own copy of a value of the type`() {
        val data = bagOf("items", listOf("book"))
        data.read<MutableList<String>>("items").orNull()?.clear()
        assertEquals(listOf("book"), data.read<List<String>>("items").orNull())
    }

    @Test
    fun `the sample reads a parcel's weight`() {
        assertEquals(1200L, reads(bagOf("parcel", Parcel(1200L, listOf("book")))))
        assertEquals(0L, reads(bagOf("parcel", "not a parcel")))
    }
}
