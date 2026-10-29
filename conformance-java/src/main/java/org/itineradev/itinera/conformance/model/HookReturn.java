package org.itineradev.itinera.conformance.model;

/** How a scripted hook ends. */
public sealed interface HookReturn {
  /** It keeps the default, or, for a workflow hook, returns. */
  record KeepDefault() implements HookReturn {}

  /** It returns {@code FinishWorkflow}. */
  record FinishWorkflow() implements HookReturn {}

  /**
   * It returns {@code FailWorkflow}.
   *
   * @param code the reason's code
   */
  record FailWorkflow(String code) implements HookReturn {}

  /** A workflow hook returns a value, which is never read. */
  record Value() implements HookReturn {}

  /** A step hook returns {@code null}, not a lifecycle. */
  record NullLike() implements HookReturn {}

  /** A step hook returns what is neither {@code keepDefault()} nor a lifecycle, a string. */
  record NotALifecycle() implements HookReturn {}

  /**
   * It fails.
   *
   * @param message its message, {@code null} when the case writes none
   */
  record Fails(String message) implements HookReturn {}
}
