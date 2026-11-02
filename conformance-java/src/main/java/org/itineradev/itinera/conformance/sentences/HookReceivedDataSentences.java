package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.sentences.HookRequests.stepData;
import static org.itineradev.itinera.conformance.sentences.HookRequests.workflowData;
import static org.itineradev.itinera.conformance.sentences.HooksReceived.received;

import io.cucumber.java.en.Then;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.step.Reason;

/** The sentences that read the data and the failure reason each hook received. */
public final class HookReceivedDataSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookReceivedDataSentences(World world) {
    this.world = world;
  }

  @Then("^the hook \"([^\"]*)\" of policy \"([^\"]*)\" received step data \"([^\"]*)\" = (.+)$")
  public void receivedStepData(String hook, String policy, String key, String written) {
    received(world, hook, policy, stepData(key), value(written));
  }

  @Then("the hook {string} of policy {string} received step data {string} absent")
  public void receivedStepDataAbsent(String hook, String policy, String key) {
    received(world, hook, policy, stepData(key), null);
  }

  @Then(
      "^the hook \"([^\"]*)\" of policy \"([^\"]*)\" received data from the workflow \"([^\"]*)\" = (.+)$")
  public void receivedWorkflowData(String hook, String policy, String key, String written) {
    received(world, hook, policy, workflowData(key), value(written));
  }

  @Then("the hook {string} of policy {string} received data from the workflow {string} absent")
  public void receivedWorkflowDataAbsent(String hook, String policy, String key) {
    received(world, hook, policy, workflowData(key), null);
  }

  @Then("the hook {string} of policy {string} received no failure reason")
  public void receivedNoFailureReason(String hook, String policy) {
    received(world, hook, policy, HookRequests::isReason, null);
  }

  @Then(
      "^the hook \"([^\"]*)\" of policy \"([^\"]*)\" received the failure reason with code"
          + " \"([^\"]*)\", message \"([^\"]*)\" and details (\\{.*\\})$")
  public void receivedTheFailureReason(
      String hook, String policy, String code, String message, String details) {
    received(
        world, hook, policy, HookRequests::isReason, new Reason(code, message, value(details)));
  }
}
