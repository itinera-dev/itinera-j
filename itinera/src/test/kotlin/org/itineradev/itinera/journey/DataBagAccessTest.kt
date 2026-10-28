package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.samples.access
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class DataBagAccessTest {
    @ParameterizedTest
    @FieldSource("discounts")
    fun `an access reads the data bag as the data bag itself does`(data: DataBag, discount: Long) {
        assertEquals(discount, access(DataBagAccess(data)))
    }

    @Test
    fun `an access gives the adapter its own copy, which it may change`() {
        val access = DataBagAccess(DataBags.of("notes", listOf("fragile")))
        (access.read("notes", notes).orNull() as MutableList<String>).add("urgent")
        assertEquals(Read.Present(listOf("fragile")), access.read("notes", notes))
    }

    @Test
    fun `once its call has returned, an access shows nothing`() {
        val access = DataBagAccess(DataBags.of("discount", 5L))
        access.close()
        assertEquals(Read.Absent, access.read("discount", TypeToken.of(Long::class.java)))
    }

    private val notes = object : TypeToken<List<String>>() {}

    companion object {
        @JvmField
        val discounts =
            listOf(
                argumentSet("a discount", DataBags.of("discount", 5L), 5L),
                argumentSet("no discount", DataBags.of(), 0L),
                argumentSet("a discount of another type", DataBags.of("discount", "5"), 0L),
            )
    }
}
