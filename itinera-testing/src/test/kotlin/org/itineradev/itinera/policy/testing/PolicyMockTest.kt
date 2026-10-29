package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.testing.samples.endings
import org.itineradev.itinera.policy.testing.samples.failingToBuild
import org.itineradev.itinera.policy.testing.samples.onFailure
import org.itineradev.itinera.policy.testing.samples.onSuccess
import org.itineradev.itinera.policy.testing.samples.onWorkflow
import org.itineradev.itinera.policy.testing.samples.stepPolicies
import org.itineradev.itinera.policy.testing.samples.workflowPolicies
import org.itineradev.itinera.step.Reason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class PolicyMockTest {
    @ParameterizedTest
    @FieldSource("successes")
    fun `on step success returns what its ending says`(ending: HookEnding, returned: OnSuccess) {
        val mock = StepPolicyScript.keepingDefault().onStepSuccess(ending).build()
        assertEquals(returned, mock.onStepSuccess())
    }

    @ParameterizedTest
    @FieldSource("failures")
    fun `a failure hook returns what its ending says`(
        ending: FailureHookEnding,
        returned: OnFailure,
    ) {
        val mock =
            StepPolicyScript.keepingDefault()
                .onStepFailure(ending)
                .onStepRetry(ending)
                .onStepAbnormalTermination(ending)
                .build()
        assertEquals(
            List(3) { returned },
            listOf(mock.onStepFailure(), mock.onStepRetry(), mock.onStepAbnormalTermination()),
        )
    }

    @Test
    fun `a hook scripted to fail throws the scripted message`() {
        val mock =
            StepPolicyScript.keepingDefault()
                .onStepSuccess(HookEnding.fails("late"))
                .onStepRetry(HookEnding.fails("down"))
                .build()
        assertEquals(
            listOf("late", "down"),
            listOf(
                assertThrows<RuntimeException> { mock.onStepSuccess() }.message,
                assertThrows<RuntimeException> { mock.onStepRetry() }.message,
            ),
        )
    }

    @Test
    fun `a workflow hook returns, or throws the scripted message`() {
        val mock =
            WorkflowPolicyScript.returning().onWorkflowFailure(HookEnding.fails("down")).build()
        mock.onWorkflowSuccess()
        assertEquals("down", assertThrows<RuntimeException> { mock.onWorkflowFailure() }.message)
    }

    @Test
    fun `every build makes a new mock`() {
        val script = StepPolicyScript.keepingDefault()
        assertNotSame(script.build(), script.build())
    }

    @Test
    fun `a script failing to build throws the scripted message on every build`() {
        val script = WorkflowPolicyScript.returning().failingToBuild("no ledger")
        repeat(2) {
            assertEquals("no ledger", assertThrows<RuntimeException> { script.build() }.message)
        }
    }

    @Test
    fun `the samples end hooks as their scripts say`() {
        assertEquals(5, endings().size)
        assertEquals(Lifecycle.FinishWorkflow, onSuccess())
        assertEquals(Lifecycle.FailWorkflow(Reason("fraud")), onFailure())
        assertEquals("the ledger is down", onWorkflow())
        assertTrue(stepPolicies().isFailed)
        assertTrue(workflowPolicies().isAborted)
        assertEquals(AbortReason.POLICY_COULD_NOT_BE_BUILT, failingToBuild())
    }

    companion object {
        private val fraud = Reason("fraud")

        @JvmField
        val successes =
            listOf(
                argumentSet("keeping the default", HookEnding.keepDefault(), KeepDefault),
                argumentSet(
                    "finishing the workflow",
                    HookEnding.finishWorkflow(),
                    Lifecycle.FinishWorkflow,
                ),
                argumentSet(
                    "failing the workflow",
                    HookEnding.failWorkflow(fraud),
                    Lifecycle.FailWorkflow(fraud),
                ),
            )

        @JvmField
        val failures =
            listOf(
                argumentSet("keeping the default", HookEnding.keepDefault(), KeepDefault),
                argumentSet(
                    "failing the workflow for a code",
                    HookEnding.failWorkflow("fraud"),
                    Lifecycle.FailWorkflow(fraud),
                ),
            )
    }
}
