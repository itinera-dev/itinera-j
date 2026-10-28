@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.policy.HookDefinition
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.samples.Messages
import org.itineradev.itinera.policy.samples.misattached
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PoliciesTest {
    @Test
    fun `policies that define one hook twice, or request a role not provided, are refused`() {
        assertEquals(
            listOf(
                Violation.StepHookDefinedTwice(
                    StepName.of("charge"),
                    StepHook.ON_STEP_SUCCESS,
                    listOf(PolicyName.of("audit"), PolicyName.of("count")),
                ),
                Violation.WorkflowHookDefinedTwice(
                    WorkflowHook.ON_WORKFLOW_FAILURE,
                    listOf(PolicyName.of("notify"), PolicyName.of("alert")),
                ),
                Violation.RoleNotProvided(PolicyName.of("notify"), Messages::class.java),
            ),
            misattached(),
        )
    }

    @Test
    fun `a role that extends the role requested provides it`() {
        val workflow =
            WorkflowDescriptor.builder(Loudspeaker::class.java, "orders")
                .step(succeeding("pack").build())
                .role(Loud::class.java)
                .policy(telling())
                .build()
        assertEquals(listOf(Loud::class.java), workflow.roles)
    }

    @Test
    fun `a step policy attached to several steps is refused once`() {
        val quiet = hookless("quiet")
        val refused =
            assertThrows<Violations> {
                WorkflowDescriptor.builder(Shop::class.java, "shop")
                    .step(succeeding("pack").policy(quiet).build())
                    .step(succeeding("ship").policy(quiet).build())
                    .build()
            }
        assertEquals(
            listOf(
                Violation.Misdeclared(
                    "policy quiet",
                    "defines no hook, though a policy defines at least one",
                )
            ),
            refused.violations,
        )
    }

    @Test
    fun `a step policy attached twice to one step defines each of its hooks twice`() {
        val audit =
            StepPolicyDescriptor.builder("audit", PolicyFactory {})
                .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
                .build()
        val refused =
            assertThrows<Violations> {
                WorkflowDescriptor.builder(Shop::class.java, "shop")
                    .step(succeeding("pack").policy(audit).policy(audit).build())
                    .build()
            }
        assertEquals(
            listOf(
                Violation.StepHookDefinedTwice(
                    StepName.of("pack"),
                    StepHook.ON_STEP_SUCCESS,
                    listOf(PolicyName.of("audit"), PolicyName.of("audit")),
                )
            ),
            refused.violations,
        )
    }

    private fun telling(): WorkflowPolicyDescriptor<Unit, Sync> {
        val messages = Requests.role(Messages::class.java)
        return WorkflowPolicyDescriptor.builder("notify", PolicyFactory {})
            .onWorkflowSuccess(
                listOf(messages),
                SyncWorkflowHook { _, got -> got[messages].tell("paid") },
            )
            .build()
    }

    private fun hookless(name: String): StepPolicyDescriptor<Unit, Sync> =
        object : StepPolicyDescriptor<Unit, Sync> {
            override val name: PolicyName = PolicyName.of(name)
            override val factory: PolicyFactory<Unit> = PolicyFactory {}
            override val onStepSuccess: HookDefinition<Sync, Unit, StepHookKind<Sync>, OnSuccess>? =
                null
            override val onStepFailure:
                HookDefinition<Sync, Unit, StepFailureKind<Sync>, OnFailure>? =
                null
            override val onStepRetry: HookDefinition<Sync, Unit, StepRetryKind<Sync>, OnFailure>? =
                null
            override val onStepAbnormalTermination:
                HookDefinition<Sync, Unit, StepHookKind<Sync>, OnFailure>? =
                null
        }

    @Role private interface Loud : Messages

    private class Loudspeaker : Loud {
        override fun tell(message: String) = Unit
    }
}
