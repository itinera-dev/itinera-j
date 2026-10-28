package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;

import io.cucumber.java.en.Then;
import java.util.Objects;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Ended;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.journey.JourneyResult;

/** The sentences that check how the journey ended. */
public final class JourneySentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public JourneySentences(World world) {
    this.world = world;
  }

  @Then("the journey succeeded")
  public void succeeded() {
    endedAs("succeeded");
  }

  @Then("the journey failed")
  public void failed() {
    endedAs("failed");
  }

  @Then("the journey was aborted")
  public void wasAborted() {
    endedAs("aborted");
  }

  private void endedAs(String expected) {
    var found = statusOf(world.result());
    holds(found.equals(expected), "the journey " + expected, "it " + found);
  }

  @Then("the journey was aborted with the abort reason {string}")
  public void wasAbortedWith(String reason) {
    var result = world.result();
    var found = result.abort() == null ? statusOf(result) : result.abort().reason().toString();
    holds(found.equals(reason), "the journey aborted with \"" + reason + "\"", "\"" + found + "\"");
  }

  @Then("run returned a result instead of throwing")
  public void returnedAResult() {
    var ended = world.journey().ended();
    holds(ended instanceof Ended.Returned, "run to return a result", ended.toString());
  }

  @Then("run was refused")
  public void wasRefused() {
    refusal();
  }

  @Then("run was refused with the message {string}")
  public void wasRefusedWith(String message) {
    var found = Objects.requireNonNullElse(refusal().getMessage(), "");
    holds(found.contains(message), "a refusal carrying \"" + message + "\"", "\"" + found + "\"");
  }

  private Refusal refusal() {
    var ended = world.journey().ended();
    if (ended instanceof Ended.Refused refused) {
      return refused.refusal();
    }
    if (ended instanceof Ended.Returned returned) {
      throw new Unmet("expected run to be refused; the journey " + statusOf(returned.result()));
    }
    throw new Unmet(
        "expected run to be refused; no instance was created: "
            + ((Ended.NotCreated) ended).failure().getMessage());
  }

  /** How the journey ended, as the cases name it. */
  static String statusOf(JourneyResult result) {
    if (result.isSucceeded()) {
      return "succeeded";
    }
    return result.isFailed() ? "failed" : "aborted";
  }
}
