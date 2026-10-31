package org.itineradev.itinera.conformance.model;

import org.itineradev.itinera.event.EventKind;

/** How a scripted reporter fails; a reporter with none never fails. */
public sealed interface ReporterFailure {
  /** Throws on the first event it receives. */
  record OnFirstEvent() implements ReporterFailure {}

  /**
   * Throws, with {@code message} when there is one, on every event of {@code kind}, and on no
   * other.
   *
   * @param kind the kind of event it throws on
   * @param message the message of what it throws, or {@code null} for its own
   */
  record OnEvent(EventKind kind, String message) implements ReporterFailure {}

  /**
   * Fails on every entry of global metadata under {@code key}, and on nothing else.
   *
   * @param key the key it fails on
   */
  record OnGlobalMetadata(String key) implements ReporterFailure {}
}
