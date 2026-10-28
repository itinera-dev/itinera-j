package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.ProviderRequest;
import org.itineradev.itinera.conformance.model.ValueType;

/** The sentences that script what each service provider requests, in the order written. */
public final class ProviderRequestSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ProviderRequestSentences(World world) {
    this.world = world;
  }

  private void requests(String provider, ProviderRequest request) {
    world.model().workflow().provider(provider).requests().add(request);
  }

  @Given("the service provider {string} requests the step name")
  public void requestsTheStepName(String provider) {
    requests(provider, new ProviderRequest.StepName());
  }

  @Given("the service provider {string} requests the attempt number")
  public void requestsTheAttempt(String provider) {
    requests(provider, new ProviderRequest.Attempt());
  }

  @Given("the service provider {string} requests the journey ID")
  public void requestsTheJourneyId(String provider) {
    requests(provider, new ProviderRequest.JourneyId());
  }

  @Given("the service provider {string} requests data from the workflow {string} of type {word}")
  public void requestsData(String provider, String key, String type) {
    requests(provider, new ProviderRequest.FromWorkflow(key, ValueType.named(type), false));
  }

  @Given(
      "the service provider {string} requests optional data from the workflow {string} of type {word}")
  public void requestsOptionalData(String provider, String key, String type) {
    requests(provider, new ProviderRequest.FromWorkflow(key, ValueType.named(type), true));
  }
}
