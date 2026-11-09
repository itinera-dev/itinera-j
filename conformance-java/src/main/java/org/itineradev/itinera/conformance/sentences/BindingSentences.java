package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.java.en.Given;
import java.util.List;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.AdapterAnswer;
import org.itineradev.itinera.conformance.model.BuildingBlocks;
import org.itineradev.itinera.conformance.model.ProviderScope;
import org.itineradev.itinera.conformance.model.ServiceType;

/**
 * The sentences that define input adapters and service providers before the workflow, and bind them
 * to it after.
 */
public final class BindingSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public BindingSentences(World world) {
    this.world = world;
  }

  private BuildingBlocks blocks() {
    return world.model().blocks();
  }

  @Given(
      "^the input adapter \"([^\"]*)\" returns (.+) when step \"([^\"]*)\" requests \"([^\"]*)\"$")
  public void returnsWhenStepRequests(String adapter, String written, String step, String key) {
    blocks().definedAdapter(adapter).answers(key, answerOf(written), step);
  }

  @Given("the input adapter {string} fails with {string} when step {string} requests {string}")
  public void failsWhenStepRequests(String adapter, String message, String step, String key) {
    blocks().definedAdapter(adapter).answers(key, new AdapterAnswer.Fails(message), step);
  }

  @Given("the workflow binds the input adapter {string} to the step {string}")
  public void bindsTheInputAdapterToTheStep(String adapter, String step) {
    blocks().bindAdapter(adapter, List.of(step));
  }

  @Given(
      "^the workflow binds the input adapter \"([^\"]*)\" to the steps (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void bindsTheInputAdapterToTheSteps(String adapter, String steps) {
    blocks().bindAdapter(adapter, names(steps));
  }

  @Given("^the service provider \"([^\"]*)\" makes an? (\\w+) for \"([^\"]*)\", for each attempt$")
  public void makesForEachAttempt(String provider, String type, String key) {
    blocks().defineProvider(provider, key, ServiceType.named(type), ProviderScope.ATTEMPT);
  }

  @Given("^the service provider \"([^\"]*)\" makes an? (\\w+) for \"([^\"]*)\", once per journey$")
  public void makesOncePerJourney(String provider, String type, String key) {
    blocks().defineProvider(provider, key, ServiceType.named(type), ProviderScope.JOURNEY);
  }

  @Given("the workflow binds the service provider {string}")
  public void bindsTheServiceProvider(String provider) {
    blocks().bindProvider(provider, null);
  }

  @Given("the workflow binds the service provider {string} to the step {string}")
  public void bindsTheServiceProviderToTheStep(String provider, String step) {
    blocks().bindProvider(provider, List.of(step));
  }

  @Given(
      "^the workflow binds the service provider \"([^\"]*)\" to the steps"
          + " (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void bindsTheServiceProviderToTheSteps(String provider, String steps) {
    blocks().bindProvider(provider, names(steps));
  }

  private static AdapterAnswer answerOf(String written) {
    return written.equals("nothing")
        ? new AdapterAnswer.Nothing()
        : new AdapterAnswer.Returns(value(written));
  }
}
