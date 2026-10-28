@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.HookDefinition
import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.async.samples.Audit
import org.itineradev.itinera.policy.async.samples.asyncAudit
import org.itineradev.itinera.policy.async.samples.asyncLedger
import org.itineradev.itinera.policy.async.samples.asyncPolicies
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.workflow.Async
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AsyncPoliciesTest {
    private val audit = asyncAudit()
    private val ledger = asyncLedger()

    @Test
    fun `an asynchronous step keeps its policies and its workflow its own`() {
        val shop = asyncPolicies()
        assertEquals(PolicyName.of("audit"), shop.steps.single().policies.single().name)
        assertEquals(PolicyName.of("ledger"), shop.policies.single().name)
        assertEquals(
            listOf("data from the step charged: Long", "async hook reporter"),
            audit.onStepSuccess?.requests?.map(Request<*, *>::toString),
        )
    }

    @ParameterizedTest
    @FieldSource("decisions")
    fun `an asynchronous step hook awaits what it calls`(reason: Reason, decided: OnFailure) =
        runBlocking<Unit> { assertEquals(decided, called(audit.onStepFailure, reason)) }

    @Test
    fun `an asynchronous workflow hook is called on its policy`() =
        runBlocking<Unit> {
            val contributed = mutableListOf<Pair<String, Any?>>()
            val hook = checkNotNull(ledger.onWorkflowSuccess)
            val answers =
                listOf(
                    JourneyId.of("order-1"),
                    Contributor { key, value -> contributed += key to value },
                )
            (hook.call as AsyncWorkflowHook).call(
                ledger.factory.build(),
                Answers(hook.requests.zip(answers).toMap()),
            )
            assertEquals(listOf("ledger" to "paid order-1"), contributed)
        }

    private suspend fun <K, R> called(
        definition: HookDefinition<Async, Audit, K, R>?,
        vararg answers: Any?,
    ): R {
        val hook = checkNotNull(definition)
        return (hook.call as AsyncHook).call(Audit(), Answers(hook.requests.zip(answers).toMap()))
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
