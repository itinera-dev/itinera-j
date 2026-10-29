package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.HookScripts.requests;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.ValueType;

/**
 * The sentences that script what data a hook requests: step data, data from the workflow, and what
 * the attempt left.
 */
public final class HookDataSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public HookDataSentences(World world) {
    this.world = world;
  }

  @Given("the hook {string} of policy {string} requests step data {string} of type {word}")
  public void requestsStepData(String hook, String policy, String key, String type) {
    requests(world, hook, policy, new HookRequest.StepData(key, ValueType.named(type), false));
  }

  @Given("the hook {string} of policy {string} requests optional step data {string} of type {word}")
  public void requestsOptionalStepData(String hook, String policy, String key, String type) {
    requests(world, hook, policy, new HookRequest.StepData(key, ValueType.named(type), true));
  }

  @Given(
      "the hook {string} of policy {string} requests data from the workflow {string} of type {word}")
  public void requestsWorkflowData(String hook, String policy, String key, String type) {
    requests(world, hook, policy, new HookRequest.WorkflowData(key, ValueType.named(type), false));
  }

  @Given(
      "the hook {string} of policy {string} requests optional data from the workflow {string} of type {word}")
  public void requestsOptionalWorkflowData(String hook, String policy, String key, String type) {
    requests(world, hook, policy, new HookRequest.WorkflowData(key, ValueType.named(type), true));
  }

  @Given("the hook {string} of policy {string} requests the failure reason")
  public void requestsTheFailureReason(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.FailureReason(false));
  }

  @Given("the hook {string} of policy {string} requests the optional failure reason")
  public void requestsTheOptionalFailureReason(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.FailureReason(true));
  }

  @Given("the hook {string} of policy {string} requests the error")
  public void requestsTheError(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.Error());
  }

  @Given("the hook {string} of policy {string} requests the failure cause")
  public void requestsTheFailureCause(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.FailureCause());
  }

  @Given("the hook {string} of policy {string} requests the retry cause")
  public void requestsTheRetryCause(String hook, String policy) {
    requests(world, hook, policy, new HookRequest.RetryCause());
  }
}
