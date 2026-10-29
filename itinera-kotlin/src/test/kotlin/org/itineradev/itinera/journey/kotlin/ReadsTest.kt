package org.itineradev.itinera.journey.kotlin

import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.journey.kotlin.samples.Parcel
import org.itineradev.itinera.journey.kotlin.samples.accessReads
import org.itineradev.itinera.journey.kotlin.samples.reads
import org.itineradev.itinera.value.kotlin.bagOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

    @Test
    fun `a reified read through an adapter's access reads as the data bag does`() {
        assertEquals(
            listOf("fragile"),
            accessReads(DataBags.access(bagOf("notes", listOf("fragile")))),
        )
        assertEquals(emptyList<String>(), accessReads(DataBags.access(bagOf("notes", 3L))))
    }
}
