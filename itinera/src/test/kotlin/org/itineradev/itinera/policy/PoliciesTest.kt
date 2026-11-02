package org.itineradev.itinera.policy

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.samples.Audit
import org.itineradev.itinera.policy.samples.audit
import org.itineradev.itinera.policy.samples.ledger
import org.itineradev.itinera.policy.samples.policies
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.Reason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class PoliciesTest {
    private val audit = audit()
    private val ledger = ledger()

    @Test
    fun `a step keeps its policies and a workflow its own, each with the hooks it defines`() {
        val shop = policies()
        assertEquals(PolicyName.of("audit"), shop.steps.single().policies.single().name)
        assertEquals(PolicyName.of("ledger"), shop.policies.single().name)
        assertNull(audit.onStepRetry)
        assertNull(audit.onStepAbnormalTermination)
        assertNull(ledger.onWorkflowFailure)
    }

    @Test
    fun `a hook keeps its requests in the order declared, each saying what it asks`() {
        assertEquals(
            listOf("data from the step charged: Long", "contributor"),
            audit.onStepSuccess?.requests?.map(Request<*, *>::toString),
        )
        assertEquals(listOf("reason"), audit.onStepFailure?.requests?.map(Request<*, *>::toString))
        assertEquals(
            listOf("journey ID", "contributor"),
            ledger.onWorkflowSuccess?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a step hook is called on the policy its factory builds, with its answers`() {
        val contributed = mutableListOf<Pair<String, Any?>>()
        val decided =
            called(audit.onStepSuccess, audit.factory.build(), 40L, recording(contributed))
        assertEquals(KeepDefault, decided)
        assertEquals(listOf("audit" to "charged 40"), contributed)
    }

    @ParameterizedTest
    @FieldSource("decisions")
    fun `a failure hook returns what its policy decides`(reason: Reason, decided: OnFailure) {
        assertEquals(decided, called(audit.onStepFailure, Audit(), reason))
    }

    @Test
    fun `a workflow hook is called on its policy, with its answers`() {
        val contributed = mutableListOf<Pair<String, Any?>>()
        calledOnWorkflow(
            ledger.onWorkflowSuccess,
            ledger.factory.build(),
            JourneyId.of("order-1"),
            recording(contributed),
        )
        assertEquals(listOf("ledger" to "paid order-1"), contributed)
    }

    private fun recording(contributed: MutableList<Pair<String, Any?>>): Contributor =
        Contributor { key, value ->
            contributed += key to value
        }

    companion object {
        @JvmField
        val decisions =
            listOf(
                argumentSet(
                    "a declined charge fails the workflow",
                    Reason("declined"),
                    Lifecycle.FailWorkflow(Reason("declined")),
                ),
                argumentSet("a timeout keeps the default", Reason("timed out"), KeepDefault),
            )
    }
}
