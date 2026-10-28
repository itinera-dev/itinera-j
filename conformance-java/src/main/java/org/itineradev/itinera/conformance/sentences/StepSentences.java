package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.run.Verdicts.holdsOfStream;

import io.cucumber.java.en.Then;
import java.util.Collections;
import java.util.IdentityHashMap;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Built;
import org.itineradev.itinera.conformance.trace.Status;

/** The sentences that read how each step ended, how often it was built, and whether any ran. */
public final class StepSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public StepSentences(World world) {
    this.world = world;
  }

  @Then("step {string} ended as {status} after {int} attempts")
  public void endedAsAfter(String step, Status status, int attempts) {
    var events = world.stream();
    holdsOfStream(
        Status.of(events, step) == status && Status.attemptsOf(events, step) == attempts,
        "\"" + step + "\" to end as " + status.written() + " after " + attempts + " attempts",
        events);
  }

  @Then("step {string} ended as aborted")
  public void endedAsAborted(String step) {
    var events = world.stream();
    holdsOfStream(
        Status.of(events, step) == Status.ABORTED, "\"" + step + "\" to end as aborted", events);
  }

  @Then("step {string} was built {int} times, each time as a new instance")
  public void wasBuilt(String step, int times) {
    var built = world.witness().built(step).stream().map(Built::step).toList();
    var distinct = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    distinct.addAll(built);
    holds(
        built.size() == times && distinct.size() == times,
        "\"" + step + "\" built " + times + " times, each a new instance",
        built.size() + " built, " + distinct.size() + " of them distinct");
  }

  @Then("no step ran")
  public void noStepRan() {
    var ran = world.witness().ran();
    holds(ran == 0, "no step to run", ran + " steps ran");
  }
}
