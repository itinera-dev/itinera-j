package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookReturn;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.policy.testing.FailureHookEnding;
import org.itineradev.itinera.policy.testing.HookEnding;
import org.itineradev.itinera.policy.testing.WorkflowHookEnding;

/** How each scripted hook ends, as the policy mocks play it. */
final class HookEndings {
  /** The message of a hook or a policy's build the case makes fail with none written. */
  static final String UNWRITTEN_FAILURE = "the scripted policy failed";

  private HookEndings() {}

  /** How {@code on step success} ends, as {@code returns} says. */
  static HookEnding successEnding(HookReturn returns) {
    if (returns instanceof HookReturn.FinishWorkflow) {
      return HookEnding.finishWorkflow();
    }
    if (returns instanceof HookReturn.FailWorkflow failing) {
      return HookEnding.failWorkflow(failing.code());
    }
    if (returns instanceof HookReturn.Fails fails) {
      return HookEnding.fails(messageOf(fails));
    }
    return HookEnding.keepDefault();
  }

  /** How the step hook {@code hook}, other than {@code on step success}, ends. */
  static FailureHookEnding failureEnding(HookReturn returns, Hook hook) {
    if (returns instanceof HookReturn.FinishWorkflow) {
      throw new ModelException("Java's types refuse FinishWorkflow from \"" + hook + "\"");
    }
    if (returns instanceof HookReturn.FailWorkflow failing) {
      return HookEnding.failWorkflow(failing.code());
    }
    if (returns instanceof HookReturn.Fails fails) {
      return HookEnding.fails(messageOf(fails));
    }
    return HookEnding.keepDefault();
  }

  /** How the workflow hook {@code hook} ends, as {@code returns} says. */
  static WorkflowHookEnding workflowEnding(HookReturn returns, Hook hook) {
    if (returns instanceof HookReturn.FinishWorkflow
        || returns instanceof HookReturn.FailWorkflow) {
      throw new ModelException("Java's types refuse a lifecycle from \"" + hook + "\"");
    }
    if (returns instanceof HookReturn.Fails fails) {
      return HookEnding.fails(messageOf(fails));
    }
    return HookEnding.keepDefault();
  }

  private static String messageOf(HookReturn.Fails fails) {
    return fails.message() == null ? UNWRITTEN_FAILURE : fails.message();
  }
}
