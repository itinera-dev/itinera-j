package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.JourneySentences.statusOf;

import io.cucumber.java.en.Then;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.JourneyFailed;
import org.itineradev.itinera.journey.Failure;
import org.itineradev.itinera.step.Reason;

/** The sentences that read the result's failure. */
public final class FailureSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public FailureSentences(World world) {
    this.world = world;
  }

  @Then("the result's failure reason has the code {string}")
  public void failureReasonHasTheCode(String code) {
    var found = failure();
    holds(
        code.equals(codeOf(found.reason())),
        "a failure whose reason has the code \"" + code + "\"",
        found.toString());
  }

  @Then("the result's failure has the cause {string}")
  public void failureHasTheCause(String cause) {
    var found = failure().cause().toString();
    holds(found.equals(cause), "a failure with the cause \"" + cause + "\"", "\"" + found + "\"");
  }

  @Then("the result's failure carries the error {string}")
  public void failureCarriesTheError(String message) {
    var found = failure().errorMessage();
    holds(
        message.equals(found),
        "a failure carrying the error \"" + message + "\"",
        String.valueOf(found));
  }

  @Then("the result's failure carries no error")
  public void failureCarriesNoError() {
    var found = failure().error();
    holds(found == null, "a failure carrying no error", String.valueOf(found));
  }

  @Then("the result's failure carries no reason")
  public void failureCarriesNoReason() {
    var found = failure().reason();
    holds(found == null, "a failure carrying no reason", String.valueOf(found));
  }

  @Then("the result names the failed step {string} with the code {string}")
  public void namesTheFailedStep(String step, String code) {
    var found = failure();
    var events = world.stream();
    holdsOfStream(
        code.equals(codeOf(found.reason()))
            && events.stream().anyMatch(event -> failsAt(event, step)),
        "a failure with the code \"" + code + "\", whose journey_failed names \"" + step + "\"",
        events);
  }

  private Failure failure() {
    var result = world.result();
    if (result.failure() == null) {
      throw new Unmet("expected the journey to fail; it " + statusOf(result));
    }
    return result.failure();
  }

  private static String codeOf(Reason reason) {
    return reason == null ? null : reason.code();
  }

  private static boolean failsAt(Event event, String step) {
    return event instanceof JourneyFailed failed && failed.step().toString().equals(step);
  }
}
