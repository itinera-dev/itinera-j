package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The building blocks a scenario defines: its policies, input adapters and service providers. A
 * scenario may define them before the workflow and bind them to it after, or declare them in the
 * workflow directly; the input adapters and providers the workflow declares are those bound, in the
 * order they were bound.
 */
public final class BuildingBlocks {
  private final Map<String, PolicyModel> policies = new LinkedHashMap<>();
  private final List<AdapterModel> adapters = new ArrayList<>();
  private final List<ProviderModel> providers = new ArrayList<>();
  private final Map<String, AdapterModel> unboundAdapters = new LinkedHashMap<>();
  private final Map<String, ProviderModel> unboundProviders = new LinkedHashMap<>();

  BuildingBlocks() {}

  /** The policies the scenario defines, by name, in the order first written. */
  public Map<String, PolicyModel> policies() {
    return policies;
  }

  /**
   * The input adapters the workflow declares, in the order declared, two of one name among them.
   */
  public List<AdapterModel> adapters() {
    return adapters;
  }

  /**
   * The service providers the workflow declares, in the order declared, two of one name among them.
   */
  public List<ProviderModel> providers() {
    return providers;
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

  /** Declares an input adapter attached to {@code steps}, after those already declared. */
  public void adapt(String name, List<String> steps) {
    adapters.add(new AdapterModel(name, steps));
  }

  /** Declares a service provider, after those already declared. */
  public void provide(String name, String key, ServiceType type, ProviderScope scope) {
    providers.add(new ProviderModel(name, key, type, scope));
  }

  /**
   * The one input adapter named {@code name}, declared or defined, defining it, attached to no step
   * yet, when there is none.
   */
  public AdapterModel definedAdapter(String name) {
    if (adapters.stream().noneMatch(adapter -> isNamed(adapter, name))) {
      return unboundAdapters.computeIfAbsent(name, undefined -> new AdapterModel(name, List.of()));
    }
    return adapter(name);
  }

  /**
   * Declares the input adapter defined as {@code name}, or a new one when none is defined, attached
   * to {@code steps}, after those already declared.
   */
  public void bindAdapter(String name, List<String> steps) {
    var adapter = unboundAdapters.remove(name);
    if (adapter == null) {
      adapter = new AdapterModel(name, List.of());
    }
    adapter.attach(steps);
    adapters.add(adapter);
  }

  /** Defines a service provider, which a binding then declares, once. */
  public void defineProvider(String name, String key, ServiceType type, ProviderScope scope) {
    if (unboundProviders.containsKey(name)
        || providers.stream().anyMatch(provider -> isNamed(provider, name))) {
      throw new ModelException("the provider \"" + name + "\" is defined twice");
    }
    unboundProviders.put(name, new ProviderModel(name, key, type, scope));
  }

  /**
   * Declares the service provider defined as {@code name}, attached to {@code steps}, or to no step
   * when they are {@code null}, after those already declared.
   */
  public void bindProvider(String name, List<String> steps) {
    var provider = unboundProviders.remove(name);
    if (provider == null) {
      throw new ModelException("no provider \"" + name + "\" is defined to be bound");
    }
    if (steps != null) {
      provider.servesOnly(steps);
    }
    providers.add(provider);
  }

  /** The one input adapter declared or defined under {@code name}. */
  public AdapterModel adapter(String name) {
    var named =
        Stream.concat(adapters.stream(), unboundAdapters.values().stream())
            .filter(adapter -> isNamed(adapter, name))
            .toList();
    if (named.size() != 1) {
      throw new ModelException(
          "the workflow declares no adapter, or several, named \"" + name + "\"");
    }
    return named.get(0);
  }

  /** The one provider declared or defined under {@code name}. */
  public ProviderModel provider(String name) {
    var named =
        Stream.concat(providers.stream(), unboundProviders.values().stream())
            .filter(provider -> isNamed(provider, name))
            .toList();
    if (named.size() != 1) {
      throw new ModelException(
          "the workflow declares no provider, or several, named \"" + name + "\"");
    }
    return named.get(0);
  }

  private static boolean isNamed(AdapterModel adapter, String name) {
    return adapter.name().equals(name);
  }

  private static boolean isNamed(ProviderModel provider, String name) {
    return provider.name().equals(name);
  }
}
