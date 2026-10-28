package org.itineradev.itinera.conformance.model;

import java.util.List;

/**
 * A service provider the workflow declares: its name, the key and type of the service it supplies,
 * its scope, and the steps it serves only, each as often as named, or every step until a sentence
 * says otherwise.
 */
public final class ProviderModel {
  private final String name;
  private final String key;
  private final ServiceType type;
  private final ProviderScope scope;
  private List<String> steps;

  ProviderModel(String name, String key, ServiceType type, ProviderScope scope) {
    this.name = name;
    this.key = key;
    this.type = type;
    this.scope = scope;
  }

  /** Its name. */
  public String name() {
    return name;
  }

  /** The key of the service it supplies. */
  public String key() {
    return key;
  }

  /** The type of the service it supplies. */
  public ServiceType type() {
    return type;
  }

  /** How long an instance it makes is lent. */
  public ProviderScope scope() {
    return scope;
  }

  /** The steps it serves only, each as often as named, or none when it serves every step. */
  public List<String> steps() {
    return steps == null ? List.of() : steps;
  }

  /** Attaches the provider to {@code served}, once. */
  public void servesOnly(List<String> served) {
    if (steps != null) {
      throw new ModelException("the steps the provider \"" + name + "\" serves are stated twice");
    }
    steps = List.copyOf(served);
  }
}
