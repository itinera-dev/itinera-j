package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.HookScripts.ends;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookReturn;

/** The sentences that script how a hook ends. */
public final class HookEndingSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookEndingSentences(World world) {
    this.world = world;
  }

  @Given("the hook {string} of policy {string} returns FinishWorkflow")
  public void returnsFinishWorkflow(String hook, String policy) {
    ends(world, hook, policy, new HookReturn.FinishWorkflow());
  }

  @Given("the hook {string} of policy {string} returns FailWorkflow with code {string}")
  public void returnsFailWorkflow(String hook, String policy, String code) {
    ends(world, hook, policy, new HookReturn.FailWorkflow(code));
  }

  @Given("the hook {string} of policy {string} throws")
  public void throwsAnError(String hook, String policy) {
    ends(world, hook, policy, new HookReturn.Fails(null));
  }

  @Given("the hook {string} of policy {string} fails with {string}")
  public void failsWith(String hook, String policy, String message) {
    ends(world, hook, policy, new HookReturn.Fails(message));
  }
}
