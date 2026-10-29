package org.itineradev.itinera.policy

import org.itineradev.itinera.policy.samples.decisions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DecisionsTest {
    @Test
    fun `a step hook keeps the default, finishes the workflow or fails it, as its kind allows`() {
        assertEquals(
            listOf(
                "keeps the default",
                "finishes the workflow",
                "fails the workflow: over the limit",
                "fails the workflow: over the limit",
                "keeps the default",
                "fails the workflow: declined",
                "fails the workflow: declined",
            ),
            decisions(),
        )
    }
}
