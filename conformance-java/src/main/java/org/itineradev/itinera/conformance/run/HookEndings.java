package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookReturn;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.policy.SyncHook;
import org.itineradev.itinera.policy.SyncWorkflowHook;
import org.itineradev.itinera.policy.testing.FailureHookEnding;
import org.itineradev.itinera.policy.testing.HookEnding;
import org.itineradev.itinera.policy.testing.WorkflowHookEnding;

/** How each scripted hook ends, as the policy mocks play it. */
final class HookEndings {
  /** The message of a hook or a policy's build the case makes fail with none written. */
  static final String UNWRITTEN_FAILURE = "the scripted policy failed";

  /** What a hook the case makes return what is not a lifecycle returns. */
  private static final String NOT_A_LIFECYCLE = "not a lifecycle";

  /** What a workflow hook the case makes return a value returns. */
  private static final String A_VALUE = "a value";

  /** Why a step hook the case makes return a value cannot, followed by the hook's name. */
  private static final String VALUE_REFUSED =
      "only a workflow hook is written to return a value, not ";

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
    if (returns instanceof HookReturn.Value) {
      throw new ModelException(VALUE_REFUSED + "\"on step success\"");
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
    if (returns instanceof HookReturn.Value) {
      throw new ModelException(VALUE_REFUSED + "\"" + hook + "\"");
    }
    return HookEnding.keepDefault();
  }

  /** How the workflow hook {@code hook} ends, as {@code returns} says. */
  static WorkflowHookEnding workflowEnding(HookReturn returns, Hook hook) {
    if (returns instanceof HookReturn.FinishWorkflow
        || returns instanceof HookReturn.FailWorkflow) {
      throw new ModelException("Java's types refuse a lifecycle from \"" + hook + "\"");
    }
    if (returns instanceof HookReturn.NullLike || returns instanceof HookReturn.NotALifecycle) {
      throw new ModelException(
          "Java's workflow hooks are void, so \"" + hook + "\" returns nothing");
    }
    if (returns instanceof HookReturn.Fails fails) {
      return HookEnding.fails(messageOf(fails));
    }
    return HookEnding.keepDefault();
  }

  /**
   * {@code hook}, or, when {@code returns} says the step hook returns {@code null} or what is not a
   * lifecycle, a hook that plays it, then returns that: an unchecked cast lets Java hide what is
   * not a lifecycle from the hook's type, as it lets an application.
   */
  @SuppressWarnings("unchecked")
  static <P, K, R> SyncHook<P, K, R> returning(HookReturn returns, SyncHook<P, K, R> hook) {
    if (returns instanceof HookReturn.NullLike) {
      return (policy, got) -> {
        hook.call(policy, got);
        return null;
      };
    }
    if (returns instanceof HookReturn.NotALifecycle) {
      SyncHook<P, K, Object> notALifecycle =
          (policy, got) -> {
            hook.call(policy, got);
            return NOT_A_LIFECYCLE;
          };
      return (SyncHook<P, K, R>) (SyncHook<P, K, ?>) notALifecycle;
    }
    return hook;
  }

  /**
   * {@code hook}, or, when {@code returns} says the workflow hook returns a value, a hook that
   * plays it, then returns one, which Java drops, since a workflow hook is void.
   */
  static <P, K> SyncWorkflowHook<P, K> workflowReturning(
      HookReturn returns, SyncWorkflowHook<P, K> hook) {
    if (returns instanceof HookReturn.Value) {
      return (policy, got) -> valued(() -> hook.call(policy, got));
    }
    return hook;
  }

  private static Object valued(Runnable hook) {
    hook.run();
    return A_VALUE;
  }

  private static String messageOf(HookReturn.Fails fails) {
    return fails.message() == null ? UNWRITTEN_FAILURE : fails.message();
  }
}
