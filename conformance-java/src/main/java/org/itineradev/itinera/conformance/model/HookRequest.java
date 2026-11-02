package org.itineradev.itinera.conformance.model;

/** What a scripted hook requests, in the order its sentences are written. */
public sealed interface HookRequest {
  /**
   * Data the step contributed.
   *
   * @param key the key it reads
   * @param type the type it reads it as
   * @param optional whether the hook is called without it
   */
  record StepData(String key, ValueType type, boolean optional) implements HookRequest {}

  /**
   * Data from the workflow.
   *
   * @param key the key it reads
   * @param type the type it reads it as
   * @param optional whether the hook is called without it
   */
  record WorkflowData(String key, ValueType type, boolean optional) implements HookRequest {}

  /**
   * The failure's reason.
   *
   * @param optional whether the hook is called without one
   */
  record FailureReason(boolean optional) implements HookRequest {}

  /** The error that ended the attempt. */
  record Error() implements HookRequest {}

  /** Why {@code on step failure} was called. */
  record FailureCause() implements HookRequest {}

  /** Why {@code on step retry} was called. */
  record RetryCause() implements HookRequest {}

  /** The name of the step the hook acts on. */
  record StepName() implements HookRequest {}

  /** The attempt the hook acts on. */
  record Attempt() implements HookRequest {}

  /** The journey ID. */
  record JourneyId() implements HookRequest {}

  /**
   * A role.
   *
   * @param role the role, as the cases name it
   */
  record Role(String role) implements HookRequest {}
}
