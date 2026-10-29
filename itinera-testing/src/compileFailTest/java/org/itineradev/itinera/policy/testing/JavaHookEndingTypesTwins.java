package org.itineradev.itinera.policy.testing;

/** What {@link JavaHookEndingTypesTest} refuses, written so that it compiles. */
final class JavaHookEndingTypesTwins {
  void script() {
    var audit = StepPolicyScript.keepingDefault();
    var notify = WorkflowPolicyScript.returning();
    audit.onStepFailure(HookEnding.failWorkflow("declined"));
    audit.onStepSuccess(HookEnding.finishWorkflow());
    notify.onWorkflowSuccess(HookEnding.keepDefault());
  }
}
