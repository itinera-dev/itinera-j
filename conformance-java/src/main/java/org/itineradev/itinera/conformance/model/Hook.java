package org.itineradev.itinera.conformance.model;

import org.itineradev.itinera.policy.StepHook;
import org.itineradev.itinera.policy.WorkflowHook;

/** A hook, as a case names it: a step hook or a workflow hook. */
public sealed interface Hook {
  /**
   * The step hook {@code hook}.
   *
   * @param hook the step hook
   */
  record OfStep(StepHook hook) implements Hook {
    @Override
    public String toString() {
      return hook.toString();
    }
  }

  /**
   * The workflow hook {@code hook}.
   *
   * @param hook the workflow hook
   */
  record OfWorkflow(WorkflowHook hook) implements Hook {
    @Override
    public String toString() {
      return hook.toString();
    }
  }

  /** The hook a case names. */
  static Hook named(String written) {
    var hook = HookNames.BY_NAME.get(written);
    if (hook == null) {
      throw new ModelException(written + " is not a hook of the cases");
    }
    return hook;
  }
}
