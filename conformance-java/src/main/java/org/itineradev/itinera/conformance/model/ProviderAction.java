package org.itineradev.itinera.conformance.model;

/** What a scripted provider does on every call before it returns, in the order written. */
public sealed interface ProviderAction {
  /**
   * It emits {@code kind} with {@code message} and {@code data}, {@code null} when the case gives
   * none.
   *
   * @param kind the event
   * @param message the message
   * @param data the data
   */
  record Emits(JourneyEmitted kind, String message, Object data) implements ProviderAction {}

  /**
   * It sets the global metadata {@code key} to {@code value}.
   *
   * @param key the key
   * @param value the value
   */
  record SetsGlobalMetadata(String key, Object value) implements ProviderAction {}
}
