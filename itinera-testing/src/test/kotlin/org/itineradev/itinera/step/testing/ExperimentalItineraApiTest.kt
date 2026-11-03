package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.testing.samples.optingIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExperimentalItineraApiTest {
    @Test
    @OptIn(ExperimentalItineraApi::class)
    fun `the sample reaches API not claimed yet once it opts in`() {
        assertEquals(0, optingIn().builds)
    }
}
