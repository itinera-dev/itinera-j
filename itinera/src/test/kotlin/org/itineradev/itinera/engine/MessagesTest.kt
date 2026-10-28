package org.itineradev.itinera.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessagesTest {
    @Test
    fun `an error's message is its own`() {
        assertEquals("full", messageOf(IllegalStateException("full")))
    }

    @Test
    fun `an error with no message gets a fixed text`() {
        assertEquals("an error with no message", messageOf(IllegalStateException()))
    }

    @Test
    fun `an error whose message cannot be read gets a fixed text`() {
        assertEquals("an error whose message could not be read", messageOf(Unreadable()))
    }

    private class Unreadable : RuntimeException() {
        override val message: String
            get() = throw UnsupportedOperationException("hidden")
    }
}
