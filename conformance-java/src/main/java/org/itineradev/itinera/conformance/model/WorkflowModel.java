package org.itineradev.itinera.conformance.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The workflow a scenario declares: its name, its steps in declaration order, and their scripts,
 * with the building blocks the scenario defines.
 */
public final class WorkflowModel {
  private final String name;
  private final List<String> steps;
  private final Map<String, StepScript> scripts = new LinkedHashMap<>();
  private List<String> reporters;
  private IdGenerator idGenerator;
  private final BuildingBlocks blocks;
  private List<String> workflowPolicies;
  private final Map<String, String> roles = new LinkedHashMap<>();
  private final Set<RoleOperation> failingOperations = new LinkedHashSet<>();

  /**
   * A workflow named {@code name} with {@code steps}, in declaration order, and the building blocks
   * {@code blocks} the scenario defines.
   */
  WorkflowModel(String name, List<String> steps, BuildingBlocks blocks) {
    this.name = name;
    this.blocks = blocks;
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

  /** The input adapters the workflow declares, in the order declared. */
  public List<AdapterModel> adapters() {
    return blocks.adapters();
  }

  /** The service providers the workflow declares, in the order declared. */
  public List<ProviderModel> providers() {
    return blocks.providers();
  }

  /** The policies the scenario defines, by name, in the order first written. */
  public Map<String, PolicyModel> policies() {
    return blocks.policies();
  }

  /** The workflow policies attached to the workflow, in order, or {@code null} until stated. */
  public List<String> workflowPolicies() {
    return workflowPolicies;
  }

  /** The roles the workflow provides, each with its operation, by name. */
  public Map<String, String> roles() {
    return roles;
  }

  /** The operations of roles that throw when called. */
  public Set<RoleOperation> failingOperations() {
    return failingOperations;
  }

  /** The policy the scenario defined under {@code name}. */
  public PolicyModel policy(String name) {
    return blocks.policy(name);
  }

  /** Attaches workflow policies, once. */
  public void attach(List<String> attached) {
    if (workflowPolicies != null) {
      throw new ModelException("the workflow's policies are stated twice");
    }
    workflowPolicies = List.copyOf(attached);
  }

  void reporters(List<String> listed) {
    reporters = List.copyOf(listed);
  }

  void idGenerator(IdGenerator generator) {
    idGenerator = generator;
  }
}
