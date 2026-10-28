package org.itineradev.itinera.conformance.model;

/** How long an instance a provider makes is lent. */
public enum ProviderScope {
  /** One instance for the whole journey. */
  JOURNEY,
  /** A new instance for each attempt. */
  ATTEMPT,
}
