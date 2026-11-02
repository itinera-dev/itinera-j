package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.List;

/** What a scenario scripts for one hook of a policy, filled in as it is written. */
public final class HookScript {
  private final List<HookRequest> requests = new ArrayList<>();
  private final List<HookAction> actions = new ArrayList<>();
  private HookReturn returns = new HookReturn.KeepDefault();
  private boolean returnStated;

  /** What it requests, in the order written. */
  public List<HookRequest> requests() {
    return requests;
  }

  /** What it does before it ends, in the order written. */
  public List<HookAction> actions() {
    return actions;
  }

  /** How the hook ends, once stated; it keeps the default until then. */
  public HookReturn returns() {
    return returns;
  }

  /** Says how {@code hook} of {@code policy} ends, once. */
  public void returns(Hook hook, String policy, HookReturn ending) {
    if (returnStated) {
      throw new ModelException(
          "how the hook \"" + hook + "\" of policy \"" + policy + "\" ends is stated twice");
    }
    returnStated = true;
    returns = ending;
  }
}
