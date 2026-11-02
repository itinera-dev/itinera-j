package org.itineradev.itinera.conformance.model;

/** What a scripted hook does before it ends, in the order its sentences are written. */
public sealed interface HookAction {
  /**
   * It contributes {@code value} under {@code key}.
   *
   * @param key the key
   * @param value the value
   */
  record Contributes(String key, Object value) implements HookAction {}

  /**
   * It contributes, under {@code key}, how many calls of the policy's hooks its instance has seen.
   *
   * @param key the key
   */
  record CountsCalls(String key) implements HookAction {}

  /**
   * It changes the step data it received under {@code key} to {@code value}, in place.
   *
   * @param key the key
   * @param value what it changes it to
   */
  record ChangesStepData(String key, Object value) implements HookAction {}

  /**
   * It emits {@code kind} with {@code message}.
   *
   * @param kind the event
   * @param message the message
   */
  record Emits(JourneyEmitted kind, String message) implements HookAction {}

  /**
   * It sets the global metadata {@code key} to {@code value}.
   *
   * @param key the key
   * @param value the value
   */
  record SetsGlobalMetadata(String key, Object value) implements HookAction {}

  /**
   * It calls {@code operation} of the role {@code role} it requested.
   *
   * @param role the role
   * @param operation the operation
   */
  record CallsRole(String role, String operation) implements HookAction {}

  /**
   * It contributes {@code value} under {@code key} and emits {@code step_info} {@code message}
   * through the contributor and the step reporter {@code step} kept after its attempt ended.
   *
   * @param step the step that kept them
   * @param key the key
   * @param value the value
   * @param message the message
   */
  record UsesKeptHandles(String step, String key, Object value, String message)
      implements HookAction {}
}
