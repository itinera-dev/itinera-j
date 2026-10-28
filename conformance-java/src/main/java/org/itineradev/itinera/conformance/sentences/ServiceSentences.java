package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ProviderScope;
import org.itineradev.itinera.conformance.model.RequestedService;
import org.itineradev.itinera.conformance.model.ServiceType;

/**
 * The sentences that declare the input adapters, the services each step requests, the service
 * providers and the steps each serves.
 */
public final class ServiceSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ServiceSentences(World world) {
    this.world = world;
  }

  private Model model() {
    return world.model();
  }

  @Given(
      "^the workflow declares the input adapter \"([^\"]*)\" for the steps (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void declaresTheInputAdapter(String adapter, String steps) {
    model().workflow().adapt(adapter, names(steps));
  }

  @Given("^step \"([^\"]*)\" requests the service \"([^\"]*)\" of type (\\w+)$")
  public void requestsTheService(String step, String key, String type) {
    model().step(step).services().add(new RequestedService(key, ServiceType.named(type)));
  }

  @Given(
      "^the workflow declares the service provider \"([^\"]*)\" for \"([^\"]*)\" of type (\\w+), once per journey$")
  public void declaresAJourneyProvider(String provider, String key, String type) {
    model().workflow().provide(provider, key, ServiceType.named(type), ProviderScope.JOURNEY);
  }

  @Given(
      "^the workflow declares the service provider \"([^\"]*)\" for \"([^\"]*)\" of type (\\w+), for each attempt$")
  public void declaresAnAttemptProvider(String provider, String key, String type) {
    model().workflow().provide(provider, key, ServiceType.named(type), ProviderScope.ATTEMPT);
  }

  @Given("^the service provider \"([^\"]*)\" serves only the steps (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void servesOnly(String provider, String steps) {
    model().workflow().provider(provider).servesOnly(names(steps));
  }
}
