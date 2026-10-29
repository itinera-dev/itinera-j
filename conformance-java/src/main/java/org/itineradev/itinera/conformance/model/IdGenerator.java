package org.itineradev.itinera.conformance.model;

/** Where a workflow instance's journey ID comes from. */
public sealed interface IdGenerator {
  /** The default: a UUID v4 for each instance. */
  record None() implements IdGenerator {}

  /**
   * A generator that returns this ID.
   *
   * @param id the ID it returns, {@code null} for a generator returning what is not a string
   */
  record Returns(String id) implements IdGenerator {}
}
