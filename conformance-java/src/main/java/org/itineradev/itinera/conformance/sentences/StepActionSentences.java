package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;

import io.cucumber.java.en.Given;
import java.util.AbstractMap.SimpleImmutableEntry;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.run.NotAValue;

/**
 * The sentences that script what a step does on every attempt: it contributes, and changes inputs.
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
    model().step(step).contributions().add(new SimpleImmutableEntry<>(key, value(written)));
  }

  @Given("step {string} contributes a non-value as {string}")
  public void contributesANonValue(String step, String key) {
    model().step(step).contributions().add(new SimpleImmutableEntry<>(key, NotAValue.INSTANCE));
  }

  @Given("^step \"([^\"]*)\" changes its input \"([^\"]*)\" to (.+)$")
  public void changesItsInput(String step, String key, String written) {
    model().step(step).inputChanges().put(key, value(written));
  }
}
