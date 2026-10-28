@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class PolicyDraftTest {
    @ParameterizedTest
    @FieldSource("definedAgain")
    fun `a policy defines each hook at most once`(define: () -> Unit, message: String) {
        assertEquals(message, assertThrows<IllegalStateException>(define).message)
    }

    @ParameterizedTest
    @FieldSource("hookless")
    fun `a policy defines at least one hook`(build: () -> Unit, message: String) {
        assertEquals(message, assertThrows<IllegalStateException>(build).message)
    }

    companion object {
        @JvmField
        val definedAgain =
            listOf(
                argumentSet(
                    "a step policy",
                    defining {
                        StepPolicyDescriptor.builder("audit", PolicyFactory {})
                            .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
                            .onStepSuccess(SyncHook { _, _ -> OnSuccess.finishWorkflow() })
                    },
                    "Policy audit already defines on step success.",
                ),
                argumentSet(
                    "a workflow policy",
                    defining {
                        WorkflowPolicyDescriptor.builder("notify", PolicyFactory {})
                            .onWorkflowFailure(SyncWorkflowHook { _, _ -> })
                            .onWorkflowFailure(SyncWorkflowHook { _, _ -> })
                    },
                    "Policy notify already defines on workflow failure.",
                ),
            )

        @JvmField
        val hookless =
            listOf(
                argumentSet(
                    "a step policy",
                    defining { StepPolicyDescriptor.builder("audit", PolicyFactory {}).build() },
                    "Policy audit defines no hook, though a policy defines at least one.",
                ),
                argumentSet(
                    "a workflow policy",
                    defining {
                        WorkflowPolicyDescriptor.builder("notify", PolicyFactory {}).build()
                    },
                    "Policy notify defines no hook, though a policy defines at least one.",
                ),
            )

        private fun defining(define: () -> Any) = named("define", define)
    }
}
