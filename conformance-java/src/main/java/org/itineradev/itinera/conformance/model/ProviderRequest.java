package org.itineradev.itinera.conformance.model;

/** A request a service provider declares. */
public sealed interface ProviderRequest {
  /** The name of the step being built. */
  record StepName() implements ProviderRequest {}

  /** The attempt being built. */
  record Attempt() implements ProviderRequest {}

  /** The journey's ID. */
  record JourneyId() implements ProviderRequest {}

  /**
   * Data from the workflow, read from the data bag.
   *
   * @param key the key it reads
   * @param type the type it reads it as
   * @param optional whether the provider is called without it
   */
  record FromWorkflow(String key, ValueType type, boolean optional) implements ProviderRequest {}
}
