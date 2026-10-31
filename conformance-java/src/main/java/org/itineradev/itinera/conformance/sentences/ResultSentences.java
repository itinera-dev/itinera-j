package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;
import static org.itineradev.itinera.conformance.sentences.JourneySentences.statusOf;

import io.cucumber.java.en.Then;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.conformance.trace.Status;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.JourneyFailed;
import org.itineradev.itinera.journey.Abort;
import org.itineradev.itinera.journey.FailureCause;

/** The sentences that read the result's abort and failure. */
public final class ResultSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ResultSentences(World world) {
    this.world = world;
  }

  @Then("the result's abort carries the error {string}")
  public void abortCarriesTheError(String message) {
    var found = abort();
    holds(
        found.error() != null && message.equals(found.errorMessage()),
        "an abort carrying the error \"" + message + "\"",
        found.toString());
  }

  @Then("the result's abort carries no error")
  public void abortCarriesNoError() {
    var found = abort();
    holds(found.error() == null, "an abort carrying no error", found.toString());
  }

  @Then(
      "the result names the step {string} as where the journey was aborted, with the abort reason {string}")
  public void namesTheAbortedStep(String step, String reason) {
    var found = abort();
    var events = world.stream();
    holdsOfStream(
        found.reason().toString().equals(reason)
            && events.stream().anyMatch(event -> Status.abortsAt(event, step)),
        "an abort \"" + reason + "\", whose journey_aborted names \"" + step + "\"",
        events);
  }

  private Abort abort() {
    var result = world.result();
    if (result.abort() == null) {
      throw new Unmet("expected the journey to be aborted; it " + statusOf(result));
    }
    return result.abort();
  }

  @Then("the result names the step {string} whose hook failed the journey, with the code {string}")
  public void namesTheStepWhoseHookFailed(String step, String code) {
    var failure = world.result().failure();
    var events = world.stream();
    holdsOfStream(
        failure != null
            && failure.cause() == FailureCause.FAIL_WORKFLOW
            && failure.reason() != null
            && failure.reason().code().equals(code)
            && events.stream().anyMatch(event -> names(event, step)),
        "FailWorkflow with \"" + code + "\", whose journey_failed names \"" + step + "\"",
        events);
  }

  private static boolean names(Event event, String step) {
    return event instanceof JourneyFailed failed && String.valueOf(failed.step()).equals(step);
  }
}
