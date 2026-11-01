package org.itineradev.itinera

import org.itineradev.itinera.samples.optingIn
import org.itineradev.itinera.service.ProviderName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExperimentalItineraApiTest {
    @Test
    @OptIn(ExperimentalItineraApi::class)
    fun `the sample reaches API not claimed yet once it opts in`() {
        assertEquals(ProviderName.of("stock"), optingIn())
    }
}
