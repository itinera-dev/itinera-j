package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Emitted;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.StepAction;
import org.itineradev.itinera.conformance.run.NotAValue;

/**
 * The sentences that script what a step does on every attempt: it contributes, emits, sets global
 * metadata and changes inputs.
 */
public final class StepActionSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public StepActionSentences(World world) {
    this.world = world;
  }

  private Model model() {
    return world.model();
  }

  @Given("^step \"([^\"]*)\" contributes \"([^\"]*)\" = (?!.* and then changes it to )(.+)$")
  public void contributes(String step, String key, String written) {
    model().step(step).actions().add(new StepAction.Contributes(key, value(written)));
  }

  @Given("step {string} contributes a non-value as {string}")
  public void contributesANonValue(String step, String key) {
    model().step(step).actions().add(new StepAction.Contributes(key, NotAValue.INSTANCE));
  }

  @Given("step {string} emits {word} {string}")
  public void emits(String step, String kind, String message) {
    model().step(step).actions().add(new StepAction.Emits(Emitted.named(kind), message, null));
  }

  @Given("^step \"([^\"]*)\" emits (\\w+) \"([^\"]*)\" with data (?!that is not a value$)(\\{.*)$")
  public void emitsWithData(String step, String kind, String message, String written) {
    model()
        .step(step)
        .actions()
        .add(new StepAction.Emits(Emitted.named(kind), message, value(written)));
  }

  @Given("step {string} emits {word} {string} with data that is not a value")
  public void emitsANonValue(String step, String kind, String message) {
    model()
        .step(step)
        .actions()
        .add(new StepAction.Emits(Emitted.named(kind), message, NotAValue.INSTANCE));
  }

  @Given("^step \"([^\"]*)\" sets the global metadata \"([^\"]*)\" to (?!a )(.+)$")
  public void setsGlobalMetadata(String step, String key, String written) {
    model().step(step).actions().add(new StepAction.SetsGlobalMetadata(key, value(written)));
  }

  @Given("step {string} sets the global metadata {string} to a non-value")
  public void setsGlobalMetadataToANonValue(String step, String key) {
    model().step(step).actions().add(new StepAction.SetsGlobalMetadata(key, NotAValue.INSTANCE));
  }

  @Given("step {string} ignores failures of its emit calls and carries on")
  public void ignoresEmitFailures(String step) {
    model().step(step).ignoreEmitFailures();
  }

  @Given("^step \"([^\"]*)\" changes its input \"([^\"]*)\" to (.+)$")
  public void changesItsInput(String step, String key, String written) {
    model().step(step).inputChanges().put(key, value(written));
  }
}
