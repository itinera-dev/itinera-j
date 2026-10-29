package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.sentences.HookScripts.does;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookAction;
import org.itineradev.itinera.conformance.model.JourneyEmitted;
import org.itineradev.itinera.conformance.model.ModelException;

/** The sentences that script what a hook does before it ends. */
public final class HookActionSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookActionSentences(World world) {
    this.world = world;
  }

  @Given("^the hook \"([^\"]*)\" of policy \"([^\"]*)\" contributes \"([^\"]*)\" = (.+)$")
  public void contributes(String hook, String policy, String key, String written) {
    does(world, hook, policy, new HookAction.Contributes(key, value(written)));
  }

  @Given(
      "the hook {string} of policy {string} counts its calls and contributes the count as {string}")
  public void countsItsCalls(String hook, String policy, String key) {
    does(world, hook, policy, new HookAction.CountsCalls(key));
  }

  @Given(
      "^the hook \"([^\"]*)\" of policy \"([^\"]*)\" changes the step data \"([^\"]*)\" it received to (.+)$")
  public void changesStepData(String hook, String policy, String key, String written) {
    does(world, hook, policy, new HookAction.ChangesStepData(key, value(written)));
  }

  @Given("the hook {string} of policy {string} emits {word} {string}")
  public void emits(String hook, String policy, String kind, String message) {
    does(world, hook, policy, new HookAction.Emits(JourneyEmitted.named(kind), message));
  }

  @Given(
      "^the hook \"([^\"]*)\" of policy \"([^\"]*)\" sets the global metadata \"([^\"]*)\" to (.+)$")
  public void setsGlobalMetadata(String hook, String policy, String key, String written) {
    does(world, hook, policy, new HookAction.SetsGlobalMetadata(key, value(written)));
  }

  @Given(
      "^the hook \"([^\"]*)\" of policy \"([^\"]*)\" uses the kept contributor and step reporter of"
          + " step \"([^\"]*)\" to contribute \"([^\"]*)\" = (.+) and emit step_info \"([^\"]*)\"$")
  public void usesKeptHandles(
      String hook, String policy, String step, String key, String written, String message) {
    if (!world.model().step(step).keepsHandles()) {
      throw new ModelException("\"" + step + "\" does not keep its contributor and step reporter");
    }
    does(world, hook, policy, new HookAction.UsesKeptHandles(step, key, value(written), message));
  }
}
