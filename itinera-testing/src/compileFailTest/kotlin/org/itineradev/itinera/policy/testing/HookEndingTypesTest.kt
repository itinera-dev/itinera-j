package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val SCRIPTS =
    """
    import org.itineradev.itinera.policy.testing.*

    val audit = StepPolicyScript.keepingDefault()
    val notify = WorkflowPolicyScript.returning()
    """

class HookEndingTypesTest {
    @ParameterizedTest
    @FieldSource("refusals")
    fun `a hook of a policy mock ends only as the hook itself may`(
        refused: String,
        message: String,
    ) {
        assertDoesNotCompile("$SCRIPTS val refused = $refused", message)
    }

    companion object {
        @JvmField
        val refusals =
            listOf(
                argumentSet(
                    "a failure hook that finishes the workflow",
                    "audit.onStepFailure(HookEnding.finishWorkflow())",
                    "actual type is 'HookEnding', but 'FailureHookEnding' was expected",
                ),
                argumentSet(
                    "a retry hook that finishes the workflow",
                    "audit.onStepRetry(HookEnding.finishWorkflow())",
                    "actual type is 'HookEnding', but 'FailureHookEnding' was expected",
                ),
                argumentSet(
                    "a workflow hook that fails the workflow",
                    "notify.onWorkflowSuccess(HookEnding.failWorkflow(\"late\"))",
                    "actual type is 'FailureHookEnding', but 'WorkflowHookEnding' was expected",
                ),
                argumentSet(
                    "a workflow hook that finishes the workflow",
                    "notify.onWorkflowFailure(HookEnding.finishWorkflow())",
                    "actual type is 'HookEnding', but 'WorkflowHookEnding' was expected",
                ),
            )
    }
}
