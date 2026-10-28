package org.itineradev.itinera.conformance.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The workflow a scenario declares: its name, its steps in declaration order, and their scripts.
 */
public final class WorkflowModel {
  private final String name;
  private final List<String> steps;
  private final Map<String, StepScript> scripts = new LinkedHashMap<>();
  private List<String> reporters;
  private IdGenerator idGenerator;

  /** A workflow named {@code name} with {@code steps}, in declaration order. */
  WorkflowModel(String name, List<String> steps) {
    this.name = name;
    this.steps = List.copyOf(steps);
    for (var step : steps) {
      scripts.putIfAbsent(step, new StepScript());
    }
  }

  /** Its name. */
  public String name() {
    return name;
  }

  /** Its steps, in declaration order. */
  public List<String> steps() {
    return steps;
  }

  /** The script of each step and rescue step, by name; two steps of one name share one. */
  public Map<String, StepScript> scripts() {
    return scripts;
  }

  /** The reporters the workflow lists, in order, or {@code null} until stated. */
  public List<String> reporters() {
    return reporters;
  }

  /** Where journey IDs come from, or {@code null} until stated. */
  public IdGenerator idGenerator() {
    return idGenerator;
  }

  void reporters(List<String> listed) {
    reporters = List.copyOf(listed);
  }

  void idGenerator(IdGenerator generator) {
    idGenerator = generator;
  }
}
