package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
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
  private final List<AdapterModel> adapters = new ArrayList<>();
  private final List<ProviderModel> providers = new ArrayList<>();

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

  /** Declares an input adapter attached to {@code steps}, after those already declared. */
  public void adapt(String name, List<String> steps) {
    adapters.add(new AdapterModel(name, steps));
  }

  /** Declares a service provider, after those already declared. */
  public void provide(String name, String key, ServiceType type, ProviderScope scope) {
    providers.add(new ProviderModel(name, key, type, scope));
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

  private static boolean isNamed(ProviderModel provider, String name) {
    return provider.name().equals(name);
  }
}
