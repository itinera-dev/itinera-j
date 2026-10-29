package org.itineradev.itinera.workflow

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueFactory
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.samples.rescues
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class RescueStepDescriptorTest {
    @Test
    fun `a built rescue step keeps its requests, in order`() {
        assertEquals(
            listOf("input card: String", "optional reason", "contributor"),
            rescues().rescue?.requests?.map(Request<*, *>::toString),
        )
    }

    @ParameterizedTest
    @FieldSource("declined")
    fun `a rescue step's factory builds its run from its answers`(
        reason: Reason?,
        outcome: RescueOutcome,
        contributed: List<Pair<String, Any?>>,
    ) {
        val renewal = rescues().rescue
        val (card, declined, contributor) = renewal?.requests.orEmpty()
        val renewed = mutableListOf<Pair<String, Any?>>()
        val got =
            Answers<RescueKind<Sync>>(
                mapOf(
                    card to "visa",
                    declined to reason,
                    contributor to Contributor { key, value -> renewed += key to value },
                )
            )
        val run = (renewal?.factory as SyncRescueFactory).build(got)
        assertEquals(outcome, run.run())
        assertEquals(contributed, renewed)
    }

    @Test
    fun `a step has no rescue step until it is given one`() {
        assertNull(
            StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }.build().rescue
        )
    }

    @Test
    fun `a second rescue step is refused when declared`() {
        val renewal = RescueStepDescriptor.builder { SyncRescueRun { RescueOutcome.success() } }
        val charge =
            StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
                .rescue(renewal.build())
        assertThrows<IllegalStateException> { charge.rescue(renewal.build()) }
    }

    companion object {
        @JvmField
        val declined =
            listOf(
                argumentSet(
                    "with the reason",
                    Reason("expired"),
                    RescueOutcome.Success,
                    listOf("card" to "visa renewed after expired"),
                ),
                argumentSet(
                    "without a reason",
                    null,
                    RescueOutcome.Success,
                    listOf("card" to "visa renewed after unknown"),
                ),
                argumentSet(
                    "for a stolen card",
                    Reason("stolen"),
                    RescueOutcome.failure("stolen card"),
                    emptyList<Pair<String, Any?>>(),
                ),
            )
    }
}
