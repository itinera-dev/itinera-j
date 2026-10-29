package org.itineradev.itinera.journey

import org.itineradev.itinera.journey.samples.contributors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContributorTest {
    @Test
    fun `a contributor takes each key and value it is given`() {
        val contributed = mutableListOf<Pair<String, Any?>>()
        contributors(Contributor { key, value -> contributed += key to value })
        assertEquals(listOf("charged", "receipt"), contributed.map(Pair<String, Any?>::first))
    }
}
