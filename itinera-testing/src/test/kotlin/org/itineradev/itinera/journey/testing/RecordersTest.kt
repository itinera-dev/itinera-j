@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.journey.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.testing.samples.contributions
import org.itineradev.itinera.journey.testing.samples.testDataBag
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RecordersTest {
    @Test
    fun `contributions keep the last value under each key, in the order the keys came`() {
        assertEquals(setOf("charged"), contributions())
    }

    @Test
    fun `contributions keep their own copy of what is contributed`() {
        val contributions = Contributions()
        val items = mutableListOf("book")
        contributions.contributor.contribute("items", items)
        items += "pen"
        assertEquals(listOf("book"), contributions.read("items", LIST).orNull())
    }

    @Test
    fun `a test data bag lends read access to what it was built with`() {
        assertEquals(35L, testDataBag())
    }

    @Test
    fun `adding to a test data bag leaves it as it was`() {
        val empty = TestDataBag()
        empty.with("amount", 35L)
        assertNull(empty.access.read("amount", LONG).orNull())
    }

    @Test
    fun `a test data bag refuses what is not a value`() {
        assertThrows<IllegalArgumentException> { TestDataBag().with("gateway", Any()) }
    }

    private companion object {
        val LONG: TypeToken<Long> = TypeToken.of(Long::class.javaObjectType)
        val LIST: TypeToken<List<*>> = TypeToken.of(List::class.java)
    }
}
