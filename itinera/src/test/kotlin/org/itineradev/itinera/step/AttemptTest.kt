package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.samples.attempts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class AttemptTest {
    @ParameterizedTest
    @FieldSource("belowOne")
    fun `an attempt is counted from 1`(number: Int) {
        assertThrows<IllegalArgumentException> { Attempt.of(number) }
    }

    @Test
    fun `only attempt 1 is the first`() {
        assertTrue(Attempt.of(1).isFirst)
        assertFalse(Attempt.of(2).isFirst)
    }

    @Test
    fun `attempts compare by their numbers`() {
        assertTrue(Attempt.of(1) < Attempt.of(2))
        assertEquals(Attempt.of(3), Attempt.of(3))
    }

    @Test
    fun `the sample tells the first attempt from the others`() {
        assertEquals("first try", attempts(Attempt.of(1)))
        assertEquals("try 2", attempts(Attempt.of(2)))
    }

    companion object {
        @JvmField
        val belowOne = listOf(argumentSet("zero", 0), argumentSet("a negative number", -1))
    }
}
