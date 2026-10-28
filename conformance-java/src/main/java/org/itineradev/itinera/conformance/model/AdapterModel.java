package org.itineradev.itinera.conformance.model;

import java.util.List;

/** An input adapter the workflow declares, attached to its steps, each as often as named. */
public final class AdapterModel {
  private final String name;
  private final List<String> steps;
  private boolean asynchronous;

  AdapterModel(String name, List<String> steps) {
    this.name = name;
    this.steps = List.copyOf(steps);
  }

  /** Its name. */
  public String name() {
    return name;
  }

  /** The steps it is attached to, each as often as named. */
  public List<String> steps() {
    return steps;
  }

  /** Whether the adapter runs asynchronously. */
  public boolean isAsynchronous() {
    return asynchronous;
  }

  /** Says the adapter runs asynchronously. */
  public void markAsynchronous() {
    asynchronous = true;
  }
}
