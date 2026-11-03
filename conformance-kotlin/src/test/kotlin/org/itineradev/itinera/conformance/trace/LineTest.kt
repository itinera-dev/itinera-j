package org.itineradev.itinera.conformance.trace

import org.itineradev.itinera.conformance.model.ModelException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LineTest {
    @Test
    fun `an event's line fills the columns it has`() {
        assertEquals(
            Line(
                "step_given_up",
                mapOf(
                    "step" to "charge",
                    "attempt" to "1",
                    "cause" to "abnormal termination",
                    "decided by" to "default",
                ),
                Data.None,
                emptyList(),
            ),
            lineOf(Events().givenUp("charge")),
        )
    }

    @Test
    fun `an abort's line names its code, its step and its error`() {
        assertEquals(
            mapOf("code" to "step could not be built", "step" to "charge", "error" to "no gateway"),
            lineOf(Events().aborted("charge", "no gateway")).cells,
        )
    }

    @Test
    fun `an abnormal termination carries its error's message, which no column shows`() {
        val line = lineOf(Events().terminated("charge", "gateway down"))
        assertEquals(mapOf("step" to "charge", "attempt" to "1"), line.cells)
        assertEquals(listOf("gateway down"), line.carried)
    }

    @Test
    fun `an emitted event carries its message and data`() {
        val line = lineOf(Events().info("charge", "paid", mapOf("amount" to 42L)))
        assertEquals("paid", line.cells["message"])
        assertEquals(Data.Is(mapOf("amount" to 42L)), line.data)
    }

    @Test
    fun `a row compares only the cells it fills`() {
        val events = Events()
        val lines = listOf(events.attempt("charge"), events.succeeded("charge")).map(::lineOf)
        val expected =
            listOf(
                expectedLine(
                    mapOf("event" to "attempt_started", "step" to "charge", "attempt" to null)
                ),
                expectedLine(mapOf("event" to "step_succeeded")),
            )
        assertTrue(areExactly(lines, expected))
        assertTrue(includesInOrder(lines, expected.drop(1)))
    }

    @Test
    fun `lines in another order are neither included in order nor exactly the same`() {
        val events = Events()
        val lines = listOf(events.attempt("charge"), events.succeeded("charge")).map(::lineOf)
        val reversed =
            listOf(
                expectedLine(mapOf("event" to "step_succeeded")),
                expectedLine(mapOf("event" to "attempt_started")),
            )
        assertFalse(includesInOrder(lines, reversed))
        assertFalse(areExactly(lines, reversed))
    }

    @Test
    fun `stated data must match, and none matches no data`() {
        val line = lineOf(Events().info("charge", "paid", null))
        assertTrue(
            areExactly(
                listOf(line),
                listOf(expectedLine(mapOf("event" to "step_info", "data" to "none"))),
            )
        )
        assertFalse(
            areExactly(
                listOf(line),
                listOf(expectedLine(mapOf("event" to "step_info", "data" to "1"))),
            )
        )
    }

    @Test
    fun `an event table with a column the format does not have is an error in the case`() {
        assertThrows<ModelException> {
            expectedLine(mapOf("event" to "step_succeeded", "colour" to "red"))
        }
    }
}
