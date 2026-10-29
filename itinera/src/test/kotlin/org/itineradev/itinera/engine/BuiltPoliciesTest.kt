package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Hooking.Audit
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.ByPolicy
import org.itineradev.itinera.event.PolicyCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.StepName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BuiltPoliciesTest {
    private val charge = StepName.of("charge")
    private val audit = PolicyName.of("audit")

    @Test
    fun `a step policy is built anew for each attempt, and its hooks for that attempt share it`() {
        val built = mutableListOf<Audit>()
        val called = mutableListOf<Audit>()
        val policy =
            StepPolicyDescriptor.builder("audit", PolicyFactory { Audit().also(built::add) })
                .onStepRetry(SyncHook { policy, _ -> keptDefault(called, policy) })
                .onStepAbnormalTermination(SyncHook { policy, _ -> keptDefault(called, policy) })
                .build()
        val log = Log()
        ran(
            journeyOf(
                    playing("charge", IllegalStateException("down"), null)
                        .retryBudget(1)
                        .policy(policy)
                )
                .reporter(log)
                .create()
        )
        assertEquals(2, built.size)
        assertEquals(listOf(built[0], built[0]), called)
    }

    @Test
    fun `a step policy that throws while built aborts the journey, naming the step and the policy`() {
        val thrown = IllegalStateException("no ledger")
        val policy =
            StepPolicyDescriptor.builder<Audit>("audit", PolicyFactory { throw thrown })
                .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
                .build()
        val log = Log()
        val result = ran(journeyOf(succeeding("charge").policy(policy)).reporter(log).create())
        assertEquals(PolicyCouldNotBeBuilt(charge, audit, "no ledger"), result.abort?.details)
        assertSame(thrown, result.abort?.error)
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    @Test
    fun `a step policy built as null aborts the journey with unexpected result`() {
        val policy =
            StepPolicyDescriptor.builder("audit", PolicyFactory<Audit> { returned(null) })
                .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
                .build()
        val result = ran(journeyOf(succeeding("charge").policy(policy)).create())
        assertEquals(
            UnexpectedResult(
                charge,
                ByPolicy(audit),
                "building the policy audit gave null, not the policy",
            ),
            result.abort?.details,
        )
    }

    @Test
    fun `a workflow policy is built once per journey`() {
        val built = mutableListOf<Audit>()
        val ids = mutableListOf<JourneyId>()
        val journeyId = Requests.journeyId()
        val ledger =
            WorkflowPolicyDescriptor.builder("ledger", PolicyFactory { Audit().also(built::add) })
                .onWorkflowSuccess(
                    listOf(journeyId),
                    SyncWorkflowHook { _, got -> ids += got[journeyId] },
                )
                .build()
        val descriptor = shop().step(succeeding("pack").build()).policy(ledger).build()
        ran(instanceOf(descriptor).create())
        ran(instanceOf(descriptor).create())
        assertEquals(2, built.size)
        assertEquals(2, ids.toSet().size)
    }

    @Test
    fun `a workflow policy that throws while built refuses the instance, with no event`() {
        val thrown = IllegalStateException("no ledger")
        val log = Log()
        val refusal = assertThrows<Refusal> { ran(ledgered(PolicyFactory { throw thrown }, log)) }
        assertEquals(
            Refused.WorkflowPolicyFailed(PolicyName.of("ledger"), thrown, "no ledger"),
            refusal.refused,
        )
        assertEquals(emptyList<String>(), log.kinds)
    }

    @Test
    fun `a workflow policy built as null refuses the instance`() {
        val refusal =
            assertThrows<Refusal> { ran(ledgered(PolicyFactory { returned(null) }, Log())) }
        assertEquals(Refused.WorkflowPolicyReturnedNull(PolicyName.of("ledger")), refusal.refused)
    }

    private fun ledgered(factory: PolicyFactory<Audit>, log: Log) =
        instanceOf(
                shop()
                    .step(succeeding("pack").build())
                    .policy(
                        WorkflowPolicyDescriptor.builder("ledger", factory)
                            .onWorkflowSuccess(SyncWorkflowHook { _, _ -> })
                            .build()
                    )
                    .build()
            )
            .reporter(log)
            .create()

    private fun keptDefault(called: MutableList<Audit>, policy: Audit): OnFailure {
        called += policy
        return OnFailure.keepDefault()
    }
}
