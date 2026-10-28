package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.en.When;
import org.itineradev.itinera.conformance.World;

/** The sentences that admit the workflow and run its journeys. */
public final class ActionSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ActionSentences(World world) {
    this.world = world;
  }

  @When("the workflow is admitted")
  public void isAdmitted() {
    world.admit();
  }

  @When("the workflow runs")
  public void runs() {
    world.run(1);
  }

  @When("the same executor runs the workflow twice, with a new instance each time")
  public void runsTwice() {
    world.run(2);
  }
}
