package org.itineradev.itinera.conformance.run;

/**
 * The cases' {@code meter}, an unrelated service whose one operation returns an integer reading.
 */
@FunctionalInterface
interface Meter {
  /** The meter's reading. */
  long reading();
}
