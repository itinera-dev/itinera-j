package org.itineradev.itinera.conformance.trace;

/** The data an event table says an event carries: unstated, none, or this value. */
public sealed interface Data {
  /** The table does not say. */
  record Unstated() implements Data {}

  /** The event carries no data. */
  record None() implements Data {}

  /**
   * The event carries this value.
   *
   * @param value what it carries
   */
  record Is(Object value) implements Data {}
}
