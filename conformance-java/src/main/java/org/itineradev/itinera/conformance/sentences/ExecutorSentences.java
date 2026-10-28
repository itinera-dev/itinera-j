package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;

/**
 * The sentences that declare a step's mode and which modes the executor accepts. Java writes no
 * asynchronous step, so no scenario this runner selects declares one, but it defines every sentence
 * of the cases.
 */
public final class ExecutorSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ExecutorSentences(World world) {
    this.world = world;
  }

  @Given("step {string} is asynchronous")
  public void isAsynchronous(String step) {
    world.model().step(step).markAsynchronous();
  }

  @Given("the executor accepts only synchronous steps, hooks and reporters")
  public void acceptsOnlySynchronous() {
    world.model().acceptOnlySynchronous();
  }
}
