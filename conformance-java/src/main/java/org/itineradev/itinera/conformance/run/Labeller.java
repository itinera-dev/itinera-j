package org.itineradev.itinera.conformance.run;

/** The cases' {@code labeller}, whose one operation returns its instance's label. */
@FunctionalInterface
interface Labeller {
  /** The instance's label. */
  String label();
}
