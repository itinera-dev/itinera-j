package org.itineradev.itinera.conformance;

import org.itineradev.itinera.conformance.model.Model;

/**
 * What one scenario's sentences share: the model its Given sentences fill. Cucumber makes one for
 * each scenario and gives it to every class of sentences.
 */
public final class World {
  private final Model model = new Model();

  /** What the scenario declares. */
  public Model model() {
    return model;
  }
}
