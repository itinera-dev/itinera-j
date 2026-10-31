package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.ReporterFailure;
import org.itineradev.itinera.journey.JourneyId;

/**
 * Every recording reporter of a scenario, by name, in the order each was made: one per journey for
 * a reporter the workflow lists, and one in all for the reporter a dispatcher holds.
 */
public final class Recorders {
  /** The name of the recording reporter the runner's own dispatchers hold. */
  static final String RUNNERS_RECORDER = "the runner's recorder";

  private final Map<String, ReporterFailure> failures;
  private final Map<String, List<ScenarioReporter>> made = new LinkedHashMap<>();

  /** The recorders of a scenario whose scripted reporters fail as {@code failures} says. */
  public Recorders(Map<String, ReporterFailure> failures) {
    this.failures = failures;
  }

  /** A new reporter under {@code name}, failing as the scenario scripts it, made with no ID. */
  public ScenarioReporter make(String name) {
    return make(name, null);
  }

  /**
   * A new reporter under {@code name}, failing as the scenario scripts it, made with {@code
   * journeyId} when the workflow lists it.
   */
  public ScenarioReporter make(String name, JourneyId journeyId) {
    var reporter = new ScenarioReporter(failures.get(name), journeyId);
    made.computeIfAbsent(name, unmade -> new ArrayList<>()).add(reporter);
    return reporter;
  }

  /** The reporters made under {@code name}, in the order they were made. */
  public List<ScenarioReporter> made(String name) {
    return List.copyOf(made.getOrDefault(name, List.of()));
  }

  /** Every reporter made, by name in the order each name was first made. */
  public List<ScenarioReporter> all() {
    return made.values().stream().flatMap(List::stream).toList();
  }
}
