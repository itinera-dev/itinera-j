package org.itineradev.itinera.conformance.model;

/** What a provider supplies as data with each instance it makes. */
public sealed interface Supplies {
  /** No data. */
  record Nothing() implements Supplies {}

  /**
   * This value.
   *
   * @param value the value
   */
  record Data(Object value) implements Supplies {}

  /** What is not a value. */
  record NonValue() implements Supplies {}
}
