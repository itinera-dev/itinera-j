package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.RescuedStepRequest;
import org.itineradev.itinera.conformance.model.ValueType;

/** The sentences that script what a rescue step requests of the step it rescues. */
public final class RescueSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public RescueSentences(World world) {
    this.world = world;
  }

  @Given("step {string} requests rescued step data {string} of type {word}")
  public void requestsRescuedStepData(String step, String key, String type) {
    requests(step, new HookRequest.StepData(key, ValueType.named(type), false));
  }

  @Given("step {string} requests optional rescued step data {string} of type {word}")
  public void requestsOptionalRescuedStepData(String step, String key, String type) {
    requests(step, new HookRequest.StepData(key, ValueType.named(type), true));
  }

  @Given("step {string} requests the failure reason")
  public void requestsTheFailureReason(String step) {
    requests(step, new HookRequest.FailureReason(false));
  }

  @Given("step {string} requests the error")
  public void requestsTheError(String step) {
    requests(step, new HookRequest.Error());
  }

  private void requests(String step, HookRequest request) {
    var script = world.model().step(step);
    script.ofRescuedStep().add(new RescuedStepRequest(request, script.inputs().size()));
  }
}
