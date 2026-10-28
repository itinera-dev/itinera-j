@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.kotlin.samples.reifiedRequests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinRequestsTest {
    @Test
    fun `reified requests keep Kotlin's types`() {
        assertEquals(
            listOf(
                "input parcels: List<String>",
                "optional input note: String",
                "service store: Store",
            ),
            reifiedRequests().map(Request<*, *>::toString),
        )
    }
}
