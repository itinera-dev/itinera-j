package org.itineradev.itinera.conformance.run;

/** A policy built for a scenario: its mock, and how many calls of its hooks it has seen. */
final class Played<T> {
  private final T mock;
  private long calls;

  Played(T mock) {
    this.mock = mock;
  }

  /** The mock playing the policy's script. */
  T mock() {
    return mock;
  }

  /** Counts one more call of its hooks, and gives how many it has seen. */
  long counted() {
    calls += 1;
    return calls;
  }
}
