package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class PositionTest {
    @ParameterizedTest
    @FieldSource("belowOne")
    fun `a position is counted from 1`(number: Int) {
        assertThrows<IllegalArgumentException> { Position.of(number) }
    }

    @Test
    fun `positions compare by their numbers`() {
        assertTrue(Position.of(1) < Position.of(2))
        assertEquals(Position.of(3), Position.of(3))
    }

    companion object {
        @JvmField
        val belowOne = listOf(argumentSet("zero", 0), argumentSet("a negative number", -1))
    }
}
