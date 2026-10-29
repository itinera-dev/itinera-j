package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.HookScripts.does;
import static org.itineradev.itinera.conformance.sentences.HookScripts.requests;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookAction;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.ModelException;

/** The sentences that script the facts and roles a hook requests. */
public final class HookFactSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookFactSentences(World world) {
    this.world = world;
  }

  @Given("the hook {string} of policy {string} requests the step name")
  public void requestsTheStepName(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.StepName());
  }

  @Given("the hook {string} of policy {string} requests the attempt number")
  public void requestsTheAttempt(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.Attempt());
  }

  @Given("the hook {string} of policy {string} requests the journey ID")
  public void requestsTheJourneyId(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.JourneyId());
  }

  @Given(
      "the hook {string} of policy {string} requests the role {string} and calls its operation {string}")
  public void callsTheRole(String hook, String policy, String role, String operation) {
    var provided = world.model().workflow().roles().get(role);
    if (provided != null && !provided.equals(operation)) {
      throw new ModelException(
          "the role \""
              + role
              + "\" has the operation \""
              + provided
              + "\", not \""
              + operation
              + "\"");
    }
    requests(world, hook, policy, new HookRequest.Role(role));
    does(world, hook, policy, new HookAction.CallsRole(role, operation));
  }
}
