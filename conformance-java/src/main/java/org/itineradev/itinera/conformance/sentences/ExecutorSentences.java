package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;

/**
 * The sentences that declare the mode of a step or an input adapter, and which modes the executor
 * accepts. Java writes no asynchronous step or adapter, so no scenario this runner selects declares
 * one, but it defines every sentence of the cases.
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

  @Given("the input adapter {string} is asynchronous")
  public void adapterIsAsynchronous(String adapter) {
    world.model().workflow().adapter(adapter).markAsynchronous();
  }

  @Given("the executor accepts only synchronous steps, hooks and reporters")
  public void acceptsOnlySynchronous() {
    world.model().acceptOnlySynchronous();
  }
}
