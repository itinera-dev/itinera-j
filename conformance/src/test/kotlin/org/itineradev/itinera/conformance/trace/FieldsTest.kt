package org.itineradev.itinera.conformance.trace

import java.time.Instant
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class FieldsTest {
    private fun started(id: String, at: String, workflow: String, keys: Set<String>) =
        JourneyStarted(1, Instant.parse(at), JourneyId.of(id), WorkflowName.of(workflow), keys)

    @Test
    fun `events apart from their journey ID and timestamp compare equal`() {
        assertEquals(
            apartFromIdAndTime(listOf(started("a", "2026-10-28T02:00:00Z", "orders", setOf("x")))),
            apartFromIdAndTime(listOf(started("b", "2026-10-28T03:00:00Z", "orders", setOf("x")))),
        )
    }

    @ParameterizedTest
    @FieldSource("others")
    fun `any other field tells events apart`(workflow: String, key: String) {
        assertNotEquals(
            apartFromIdAndTime(listOf(started("a", "2026-10-28T02:00:00Z", "orders", setOf("x")))),
            apartFromIdAndTime(listOf(started("a", "2026-10-28T02:00:00Z", workflow, setOf(key)))),
        )
    }

    companion object {
        @JvmField
        val others =
            listOf(
                argumentSet("another workflow", "refunds", "x"),
                argumentSet("other initial keys", "orders", "y"),
            )
    }
}
