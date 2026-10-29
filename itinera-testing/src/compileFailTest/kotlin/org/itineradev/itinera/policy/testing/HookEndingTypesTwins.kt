package org.itineradev.itinera.policy.testing

/** What [HookEndingTypesTest] refuses, written so that it compiles. */
internal object HookEndingTypesTwins {
    val audit = StepPolicyScript.keepingDefault()
    val notify = WorkflowPolicyScript.returning()

    val failing = audit.onStepFailure(HookEnding.failWorkflow("declined"))
    val retrying = audit.onStepRetry(HookEnding.keepDefault())
    val succeeding = audit.onStepSuccess(HookEnding.finishWorkflow())
    val notified = notify.onWorkflowSuccess(HookEnding.keepDefault())
    val notifying = notify.onWorkflowFailure(HookEnding.fails("no mail"))
}
