package org.itineradev.itinera.conformance.model;

import java.util.List;

/**
 * An input adapter the workflow declares.
 *
 * @param name its name
 * @param steps the steps it is attached to, each as often as named
 */
public record AdapterModel(String name, List<String> steps) {
  /** Copies {@code steps}. */
  public AdapterModel {
    steps = List.copyOf(steps);
  }
}
