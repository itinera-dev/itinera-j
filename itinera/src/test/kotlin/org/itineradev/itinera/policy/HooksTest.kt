package org.itineradev.itinera.policy

import org.itineradev.itinera.policy.samples.hooks
import org.itineradev.itinera.step.Reason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class HooksTest {
    @ParameterizedTest
    @FieldSource("names")
    fun `names print as the specification writes them`(name: Enum<*>, written: String) {
        assertEquals(written, name.toString())
    }

    @Test
    fun `the sample tells each lifecycle a hook returned`() {
        assertEquals(
            "on step retry after failure failed the workflow: fraud",
            hooks(
                StepHook.ON_STEP_RETRY,
                RetryCause.FAILURE,
                Lifecycle.FailWorkflow(Reason("fraud")),
            ),
        )
    }

    companion object {
        @JvmField
        val names =
            listOf(
                argumentSet("on step success", StepHook.ON_STEP_SUCCESS, "on step success"),
                argumentSet("on step failure", StepHook.ON_STEP_FAILURE, "on step failure"),
                argumentSet("on step retry", StepHook.ON_STEP_RETRY, "on step retry"),
                argumentSet(
                    "on step abnormal termination",
                    StepHook.ON_STEP_ABNORMAL_TERMINATION,
                    "on step abnormal termination",
                ),
                argumentSet(
                    "on workflow success",
                    WorkflowHook.ON_WORKFLOW_SUCCESS,
                    "on workflow success",
                ),
                argumentSet(
                    "on workflow failure",
                    WorkflowHook.ON_WORKFLOW_FAILURE,
                    "on workflow failure",
                ),
                argumentSet("a retry after a failure", RetryCause.FAILURE, "failure"),
                argumentSet(
                    "a retry after an abnormal termination",
                    RetryCause.ABNORMAL_TERMINATION,
                    "abnormal termination",
                ),
            )
    }
}
