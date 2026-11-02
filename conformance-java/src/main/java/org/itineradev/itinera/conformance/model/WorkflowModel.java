package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The workflow a scenario declares: its name, its steps in declaration order, and their scripts.
 */
public final class WorkflowModel {
  private final String name;
  private final List<String> steps;
  private final Map<String, StepScript> scripts = new LinkedHashMap<>();
  private List<String> reporters;
  private IdGenerator idGenerator;
  private final List<AdapterModel> adapters = new ArrayList<>();
  private final List<ProviderModel> providers = new ArrayList<>();
  private final Map<String, PolicyModel> policies = new LinkedHashMap<>();
  private List<String> workflowPolicies;
  private final Map<String, String> roles = new LinkedHashMap<>();
  private final Set<RoleOperation> failingOperations = new LinkedHashSet<>();

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

  /** The input adapters, in the order declared, two of one name among them. */
  public List<AdapterModel> adapters() {
    return adapters;
  }

  /** The service providers, in the order declared, two of one name among them. */
  public List<ProviderModel> providers() {
    return providers;
  }

  /** The policies the scenario defines, by name, in the order first written. */
  public Map<String, PolicyModel> policies() {
    return policies;
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

  /**
   * Defines {@code hook} on the policy {@code name} of {@code kind}, defining the policy the first
   * time.
   */
  public void define(String name, PolicyKind kind, Hook hook) {
    var policy = policies.computeIfAbsent(name, undefined -> new PolicyModel(name, kind));
    if (policy.kind() != kind) {
      throw new ModelException("\"" + name + "\" is a step and a workflow policy");
    }
    if (policy.hooks().containsKey(hook)) {
      throw new ModelException("\"" + name + "\" defines \"" + hook + "\" twice");
    }
    policy.hooks().put(hook, new HookScript());
  }

  /** The policy the scenario defined under {@code name}. */
  public PolicyModel policy(String name) {
    var policy = policies.get(name);
    if (policy == null) {
      throw new ModelException("no policy \"" + name + "\" is defined");
    }
    return policy;
  }

  /** Attaches workflow policies, once. */
  public void attach(List<String> attached) {
    if (workflowPolicies != null) {
      throw new ModelException("the workflow's policies are stated twice");
    }
    workflowPolicies = List.copyOf(attached);
  }

  /** Declares an input adapter attached to {@code steps}, after those already declared. */
  public void adapt(String name, List<String> steps) {
    adapters.add(new AdapterModel(name, steps));
  }

  /** Declares a service provider, after those already declared. */
  public void provide(String name, String key, ServiceType type, ProviderScope scope) {
    providers.add(new ProviderModel(name, key, type, scope));
  }

  /** The one input adapter the workflow declares under {@code name}. */
  public AdapterModel adapter(String name) {
    var named = adapters.stream().filter(adapter -> isNamed(adapter, name)).toList();
    if (named.size() != 1) {
      throw new ModelException(
          "the workflow declares no adapter, or several, named \"" + name + "\"");
    }
    return named.get(0);
  }

  /** The one provider the workflow declares under {@code name}. */
  public ProviderModel provider(String name) {
    var named = providers.stream().filter(provider -> isNamed(provider, name)).toList();
    if (named.size() != 1) {
      throw new ModelException(
          "the workflow declares no provider, or several, named \"" + name + "\"");
    }
    return named.get(0);
  }

  void reporters(List<String> listed) {
    reporters = List.copyOf(listed);
  }

  void idGenerator(IdGenerator generator) {
    idGenerator = generator;
  }

  private static boolean isNamed(AdapterModel adapter, String name) {
    return adapter.name().equals(name);
  }

  private static boolean isNamed(ProviderModel provider, String name) {
    return provider.name().equals(name);
  }
}
