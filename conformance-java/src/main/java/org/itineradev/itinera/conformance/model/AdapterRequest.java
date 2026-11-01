package org.itineradev.itinera.conformance.model;

/** A request an input adapter declares. */
public sealed interface AdapterRequest {
  /**
   * A required request for data from the workflow, read from the data bag.
   *
   * @param key the key it reads
   * @param type the type it reads it as
   */
  record FromWorkflow(String key, ValueType type) implements AdapterRequest {}

  /** Read access to the data bag. */
  record DataBag() implements AdapterRequest {}
}
