package org.itineradev.itinera.conformance.model;

import java.util.Map;

/**
 * One row of a step's attempts: how the attempt ends, the reason's code, message and details or the
 * error's message, and what it contributes first, in the order written.
 *
 * @param number the attempt's number, from 1
 * @param ending how it ends
 * @param code the reason's code, or {@code null}
 * @param message the reason's or the error's message, or {@code null}
 * @param details the reason's details, or {@code null}
 * @param contributes what it contributes before it ends, by key, in the order written
 */
public record ScriptedAttempt(
    int number,
    Ending ending,
    String code,
    String message,
    Object details,
    Map<String, Object> contributes) {
  /** An attempt that only ends, with no reason and no contribution. */
  public ScriptedAttempt(int number, Ending ending) {
    this(number, ending, null, null, null, Map.of());
  }
}
