package org.itineradev.itinera.conformance.run;

import java.util.List;

/** How the runner runs journeys of the admitted workflow. */
@FunctionalInterface
public interface Journeys {
  /** Runs {@code times} journeys, one after another, on one executor, each with a new instance. */
  List<Journey> run(int times);
}
