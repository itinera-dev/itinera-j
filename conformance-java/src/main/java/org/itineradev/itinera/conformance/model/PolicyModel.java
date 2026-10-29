package org.itineradev.itinera.conformance.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** What a scenario scripts for one policy: its kind, how it is built and each hook it defines. */
public final class PolicyModel {
  private final String name;
  private final PolicyKind kind;
  private final Map<Hook, HookScript> hooks = new LinkedHashMap<>();
  private PolicyBuild build = new PolicyBuild.Built();

  PolicyModel(String name, PolicyKind kind) {
    this.name = name;
    this.kind = kind;
  }

  /** Its name. */
  public String name() {
    return name;
  }

  /** Whether it is attached to steps or to the workflow. */
  public PolicyKind kind() {
    return kind;
  }

  /** The hooks it defines, in the order first written. */
  public Map<Hook, HookScript> hooks() {
    return hooks;
  }

  /** How building it goes. */
  public PolicyBuild build() {
    return build;
  }

  /** Says that building it fails, with {@code message} or none, once. */
  public void failsToBuild(String message) {
    builtAs(new PolicyBuild.Fails(message));
  }

  /** Says that its factory returns {@code null}, not a policy, once. */
  public void buildAsNull() {
    builtAs(new PolicyBuild.NullLike());
  }

  private void builtAs(PolicyBuild stated) {
    if (!(build instanceof PolicyBuild.Built)) {
      throw new ModelException("how the policy \"" + name + "\" fails to build is stated twice");
    }
    build = stated;
  }

  /** The script of {@code hook}, which the policy must define. */
  public HookScript hook(Hook hook) {
    var script = hooks.get(hook);
    if (script == null) {
      throw new ModelException(
          "the policy \"" + name + "\" does not define the hook \"" + hook + "\"");
    }
    return script;
  }
}
