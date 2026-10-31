@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request.java

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.java.samples.javaRequests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JavaRequestsTest {
    @Test
    fun `an Optional input is an optional input`() {
        assertEquals(
            listOf("optional input discount: Long", "contributor"),
            javaRequests().requests.map(Request<*, *>::toString),
        )
    }
}
