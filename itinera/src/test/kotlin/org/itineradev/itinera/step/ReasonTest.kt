package org.itineradev.itinera.step

import org.itineradev.itinera.step.samples.reasons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReasonTest {
    @Test
    fun `a reason given only a code says nothing more`() {
        val declined = Reason("declined")
        assertNull(declined.message)
        assertNull(declined.details)
    }

    @Test
    fun `the sample gives a reason with a message and details`() {
        assertEquals(mapOf("retryAfter" to 30L), reasons().details)
    }
}
