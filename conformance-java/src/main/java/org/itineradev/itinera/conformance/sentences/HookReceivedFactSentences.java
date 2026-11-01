package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.HooksReceived.receivedFor;
import static org.itineradev.itinera.conformance.sentences.HooksReceived.receivedWritten;

import io.cucumber.java.en.Then;
import org.itineradev.itinera.conformance.World;

/** The sentences that read the step, attempt, journey, cause and error each hook received. */
public final class HookReceivedFactSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookReceivedFactSentences(World world) {
    this.world = world;
  }

  @Then("the hook {string} of policy {string} received the step name {string}")
  public void receivedTheStepName(String hook, String policy, String step) {
    receivedWritten(world, hook, policy, HookRequests::isStepName, step);
  }

  @Then("the hook {string} of policy {string} received the attempt number {int}")
  public void receivedTheAttempt(String hook, String policy, int attempt) {
    receivedWritten(world, hook, policy, HookRequests::isAttempt, String.valueOf(attempt));
  }

  @Then("the hook {string} of policy {string} received the journey ID {string}")
  public void receivedTheJourneyId(String hook, String policy, String id) {
    receivedWritten(world, hook, policy, HookRequests::isJourneyId, id);
  }

  @Then("the hook {string} of policy {string} received the cause {string}")
  public void receivedTheCause(String hook, String policy, String cause) {
    receivedWritten(world, hook, policy, HookRequests::isCause, cause);
  }

  @Then("the hook {string} of policy {string} received an error")
  public void receivedAnError(String hook, String policy) {
    var errors = receivedFor(world, hook, policy, HookRequests::isError);
    holds(
        errors.stream().anyMatch(Throwable.class::isInstance),
        "\"" + hook + "\" of \"" + policy + "\" to receive an error",
        errors.toString());
  }

  @Then("the hook {string} of policy {string} received the error with the message {string}")
  public void receivedTheErrorWithTheMessage(String hook, String policy, String message) {
    var messages =
        receivedFor(world, hook, policy, HookRequests::isError).stream()
            .map(HookReceivedFactSentences::messageOf)
            .toList();
    holds(
        messages.contains(message),
        "\"" + hook + "\" of \"" + policy + "\" to receive an error \"" + message + "\"",
        messages.toString());
  }

  private static String messageOf(Object value) {
    return value instanceof Throwable error ? error.getMessage() : null;
  }
}
