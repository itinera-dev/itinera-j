package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Hooking.Audit
import org.itineradev.itinera.engine.Hooking.audit
import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.ByStepHook
import org.itineradev.itinera.event.InvalidLifecycle
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Sync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class LifecyclesTest {
    private val charge = StepName.of("charge")
    private val onSuccess = ByStepHook(PolicyName.of("audit"), StepHook.ON_STEP_SUCCESS)

    @Test
    fun `finishWorkflow from a hook other than on step success aborts the journey with invalid lifecycle`() {
        val finishing =
            SyncHook<Audit, StepFailureKind<Sync>, Any> { _, _ -> Lifecycle.FinishWorkflow }
        val failing =
            contributing("charge", { Outcome.failure("declined") })
                .policy(audit().onStepFailure(returned(finishing)).build())
        assertEquals(InvalidLifecycle(charge), ran(journeyOf(failing).create()).abort?.details)
    }

    @ParameterizedTest
    @FieldSource("unexpected")
    fun `what a step hook returns that is no lifecycle aborts the journey with unexpected result`(
        returns: Any?,
        said: String,
    ) {
        val other = SyncHook<Audit, StepHookKind<Sync>, Any?> { _, _ -> returns }
        val result =
            ran(
                journeyOf(
                        succeeding("charge").policy(audit().onStepSuccess(returned(other)).build())
                    )
                    .create()
            )
        assertEquals(
            UnexpectedResult(
                charge,
                onSuccess,
                "the hook on step success of policy audit returned $said, not keepDefault() or a " +
                    "lifecycle",
            ),
            result.abort?.details,
        )
    }

    @Test
    fun `failWorkflow whose reason's details are not a value aborts the journey with not a value`() {
        val details = Reason("fraud", null, Any())
        val failing =
            succeeding("charge")
                .policy(
                    audit()
                        .onStepSuccess(SyncHook { _, _ -> OnSuccess.failWorkflow(details) })
                        .build()
                )
        assertEquals(
            NotAValue.InDetails(charge, onSuccess, null),
            ran(journeyOf(failing).create()).abort?.details,
        )
    }

    companion object {
        @JvmField
        val unexpected =
            listOf(
                argumentSet("null", null, "null"),
                argumentSet("a string", "done", "an object of java.lang.String"),
            )
    }
}
