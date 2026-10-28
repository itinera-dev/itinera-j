package org.itineradev.itinera.conformance.model;

/** What the executor is given to dispatch a journey's events. */
public sealed interface Dispatching {
  /** No sentence says: the runner gives its recording dispatcher factory. */
  record Unstated() implements Dispatching {}

  /** The executor uses its default dispatcher factory. */
  record Default() implements Dispatching {}

  /** The factory fails when the executor asks it for a dispatcher. */
  record FactoryFails() implements Dispatching {}

  /**
   * Each dispatcher holds this scripted reporter, and behaves as {@code script} says.
   *
   * @param reporter the name of the reporter it holds
   * @param script how it behaves
   */
  record Holding(String reporter, DispatcherScript script) implements Dispatching {}
}
