package org.itineradev.itinera

import org.itineradev.itinera.samples.optingIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExperimentalItineraApiTest {
    @Test
    fun `the sample reaches API not claimed yet once it opts in`() {
        assertEquals("this API is not claimed yet", optingIn())
    }
}
