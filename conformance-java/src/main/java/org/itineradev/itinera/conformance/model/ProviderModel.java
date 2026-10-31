package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A service provider the workflow declares: its name, the key and type of the service it supplies,
 * its scope, the steps it serves only, each as often as named, or every step until a sentence says
 * otherwise, and what it requests and does when called.
 */
public final class ProviderModel {
  private final String name;
  private final String key;
  private final ServiceType type;
  private final ProviderScope scope;
  private List<String> steps;
  private final List<ProviderRequest> requests = new ArrayList<>();
  private String labelledBy;
  private ProviderFailure failure;
  private Implementation implementation = Implementation.FIRST;
  private Supplies supplies = new Supplies.Nothing();
  private boolean returnsNothing;
  private String servicesFail;
  private boolean asynchronous;

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

  /** What it requests, in the order written. */
  public List<ProviderRequest> requests() {
    return requests;
  }

  /** The key of the request whose string labels each instance, or {@code null}. */
  public String labelledBy() {
    return labelledBy;
  }

  /** Labels each instance with the string it received for {@code key}. */
  public void labelledBy(String key) {
    labelledBy = key;
  }

  /** When it fails, or {@code null}. */
  public ProviderFailure failure() {
    return failure;
  }

  /** Says when it fails, once. */
  public void fails(ProviderFailure stated) {
    if (failure != null) {
      throw new ModelException("how the provider \"" + name + "\" fails is stated twice");
    }
    failure = stated;
  }

  /** The implementation of {@code labeller} it makes. */
  public Implementation implementation() {
    return implementation;
  }

  /** Makes instances of {@code made}. */
  public void makes(Implementation made) {
    implementation = made;
  }

  /** What it supplies as data with each instance. */
  public Supplies supplies() {
    return supplies;
  }

  /** Supplies {@code data} with each instance. */
  public void supplies(Supplies data) {
    supplies = data;
  }

  /** Whether it returns neither an instance nor an error. */
  public boolean returnsNothing() {
    return returnsNothing;
  }

  /** Says it returns neither an instance nor an error. */
  public void returnNothing() {
    returnsNothing = true;
  }

  /** The message every operation of the instances it makes fails with, or {@code null}. */
  public String servicesFail() {
    return servicesFail;
  }

  /** Says every operation of the instances it makes fails with {@code message}. */
  public void servicesFail(String message) {
    servicesFail = message;
  }

  /** Whether it runs asynchronously. */
  public boolean isAsynchronous() {
    return asynchronous;
  }

  /** Says it runs asynchronously. */
  public void markAsynchronous() {
    asynchronous = true;
  }
}
