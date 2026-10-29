package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.HookRequests.stepData;

import io.cucumber.java.en.Then;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.run.Received;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.step.Reason;

/** The sentences that read what a rescue step received of the step it rescues. */
public final class RescueReceivedSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public RescueReceivedSentences(World world) {
    this.world = world;
  }

  @Then("^step \"([^\"]*)\" was built with rescued step data \"([^\"]*)\" = (.+)$")
  public void wasBuiltWithRescuedStepData(String step, String key, String written) {
    received(step, stepData(key), value(written));
  }

  @Then("step {string} was built with rescued step data {string} absent")
  public void wasBuiltWithRescuedStepDataAbsent(String step, String key) {
    received(step, stepData(key), null);
  }

  @Then(
      "^step \"([^\"]*)\" received the failure reason with code \"([^\"]*)\", message"
          + " \"([^\"]*)\" and details (\\{.*\\})$")
  public void receivedTheFailureReason(String step, String code, String message, String details) {
    received(step, HookRequests::isReason, new Reason(code, message, value(details)));
  }

  @Then("step {string} received the error with the message {string}")
  public void receivedTheError(String step, String message) {
    var error = receivedBy(step, HookRequests::isError);
    holds(
        error instanceof Throwable thrown && message.equals(thrown.getMessage()),
        "\"" + step + "\" to receive the error with the message \"" + message + "\"",
        String.valueOf(error));
  }

  private void received(String step, Predicate<HookRequest> asked, Object expected) {
    var found = receivedBy(step, asked);
    holds(
        Objects.equals(found, expected),
        "\"" + step + "\" to receive " + expected,
        String.valueOf(found));
  }

  /** What the rescue step {@code step} received for its request that {@code asked} picks. */
  private Object receivedBy(String step, Predicate<HookRequest> asked) {
    var received = builtOnce(step).ofRescuedStep();
    var request =
        received.keySet().stream()
            .filter(asked)
            .findFirst()
            .orElseThrow(() -> unrequested(step, received.keySet()));
    return received.get(request);
  }

  private static Unmet unrequested(String step, Set<HookRequest> requested) {
    return new Unmet("expected \"" + step + "\" to request it; it requested " + requested);
  }

  private Received builtOnce(String step) {
    var built = world.witness().built(step);
    if (built.isEmpty()) {
      throw new Unmet("expected \"" + step + "\" built; it was not");
    }
    return built.get(0).received();
  }
}
