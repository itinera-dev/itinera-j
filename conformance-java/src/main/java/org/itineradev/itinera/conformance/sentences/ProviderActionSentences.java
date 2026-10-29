package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.JourneyEmitted;
import org.itineradev.itinera.conformance.model.ProviderAction;
import org.itineradev.itinera.conformance.run.NotAValue;

/** The sentences that script what a service provider does on every call before it returns. */
public final class ProviderActionSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ProviderActionSentences(World world) {
    this.world = world;
  }

  private void does(String provider, ProviderAction action) {
    world.model().workflow().provider(provider).actions().add(action);
  }

  @Given("the service provider {string} emits {word} {string}")
  public void emits(String provider, String kind, String message) {
    does(provider, new ProviderAction.Emits(JourneyEmitted.named(kind), message, null));
  }

  @Given("the service provider {string} emits {word} {string} with data that is not a value")
  public void emitsANonValue(String provider, String kind, String message) {
    does(
        provider,
        new ProviderAction.Emits(JourneyEmitted.named(kind), message, NotAValue.INSTANCE));
  }

  @Given("^the service provider \"([^\"]*)\" sets the global metadata \"([^\"]*)\" to (.+)$")
  public void setsGlobalMetadata(String provider, String key, String written) {
    does(provider, new ProviderAction.SetsGlobalMetadata(key, value(written)));
  }
}
