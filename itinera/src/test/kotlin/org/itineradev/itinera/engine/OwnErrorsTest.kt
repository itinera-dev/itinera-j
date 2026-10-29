package org.itineradev.itinera.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OwnErrorsTest {
    @Test
    fun `nothing changed in a copy reaches the error it copies`() {
        val error = IllegalStateException("down")
        val copy = ownCopyOf(error)
        copy.stackTrace = emptyArray()
        copy.addSuppressed(IllegalArgumentException("later"))
        assertEquals(0, error.suppressed.size)
        assertTrue(error.stackTrace.isNotEmpty())
    }

    @Test
    fun `an error that cannot be serialised is its own copy`() {
        val error = Unserialisable()
        assertSame(error, ownCopyOf(error))
    }

    @Test
    fun `an Error thrown while copying is never caught`() {
        assertThrows<OutOfMemoryError> { ownCopyOf(ExhaustingWhenWritten()) }
    }

    /** An error holding a field that Java serialisation cannot write. */
    private class Unserialisable(val lock: Any = Any()) : RuntimeException("down")

    /** An error whose serialisation throws an [Error]. */
    private class ExhaustingWhenWritten : RuntimeException("down") {
        fun writeReplace(): Any = throw OutOfMemoryError("full")
    }
}
