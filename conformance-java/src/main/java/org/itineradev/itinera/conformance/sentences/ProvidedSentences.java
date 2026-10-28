package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;

import io.cucumber.java.en.Then;
import java.util.Map;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.ProviderRequest;
import org.itineradev.itinera.step.Attempt;

/** The sentences that read how often each service provider was called, and what it received. */
public final class ProvidedSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ProvidedSentences(World world) {
    this.world = world;
  }

  @Then("the service provider {string} was called {int} times")
  public void wasCalled(String provider, int times) {
    var calls = world.witness().calls(provider).size();
    holds(calls == times, "\"" + provider + "\" called " + times + " times", calls + " calls");
  }

  @Then("the service provider {string} received the step name {string}")
  public void receivedTheStepName(String provider, String step) {
    receivedOnACall(provider, new ProviderRequest.StepName(), step);
  }

  @Then("the service provider {string} received the attempt number {int}")
  public void receivedTheAttempt(String provider, int attempt) {
    receivedOnACall(provider, new ProviderRequest.Attempt(), Attempt.of(attempt).toString());
  }

  @Then("the service provider {string} received the journey ID {string}")
  public void receivedTheJourneyId(String provider, String id) {
    receivedOnACall(provider, new ProviderRequest.JourneyId(), id);
  }

  @Then("the service provider {string} received data from the workflow {string} absent")
  public void receivedDataAbsent(String provider, String key) {
    var calls = world.witness().calls(provider);
    holds(
        calls.stream().anyMatch(received -> isAbsentIn(received, key)),
        "\"" + provider + "\" to receive \"" + key + "\" absent",
        "the calls " + calls);
  }

  private void receivedOnACall(String provider, ProviderRequest request, String written) {
    var calls = world.witness().calls(provider);
    holds(
        calls.stream().anyMatch(received -> wasGiven(received, request, written)),
        "\"" + provider + "\" to receive " + written + " for " + request,
        "the calls " + calls);
  }

  private static boolean wasGiven(
      Map<ProviderRequest, Object> received, ProviderRequest request, String written) {
    return received.get(request) != null && received.get(request).toString().equals(written);
  }

  private static boolean isAbsentIn(Map<ProviderRequest, Object> received, String key) {
    return received.entrySet().stream().anyMatch(entry -> isAbsentFor(entry, key));
  }

  private static boolean isAbsentFor(Map.Entry<ProviderRequest, Object> entry, String key) {
    return entry.getKey() instanceof ProviderRequest.FromWorkflow request
        && request.key().equals(key)
        && entry.getValue() == null;
  }
}
