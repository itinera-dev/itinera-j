package org.itineradev.itinera.conformance.model;

/** One thing a scripted step does on every attempt before it ends, as a case writes it. */
public sealed interface StepAction {
  /**
   * It contributes {@code value} under {@code key}.
   *
   * @param key the key
   * @param value the value
   */
  record Contributes(String key, Object value) implements StepAction {}

  /**
   * It emits {@code kind} with {@code message} and {@code data}, {@code null} when the case gives
   * none.
   *
   * @param kind the event
   * @param message the message
   * @param data the data
   */
  record Emits(Emitted kind, String message, Object data) implements StepAction {}

  /**
   * It sets the global metadata {@code key} to {@code value}.
   *
   * @param key the key
   * @param value the value
   */
  record SetsGlobalMetadata(String key, Object value) implements StepAction {}
}
