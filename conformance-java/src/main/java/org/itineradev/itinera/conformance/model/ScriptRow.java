package org.itineradev.itinera.conformance.model;

import java.util.Map;

/**
 * One row of the script.
 *
 * @param step the step it names
 * @param attempt the attempts it chooses, as written, {@code null} when the first row of a step
 *     leaves attempt 1 unsaid
 * @param contributes what they contribute, in the order written
 * @param emits the event they emit, {@code null} for none
 * @param emitMessage the message of that event
 * @param setsGlobalMetadata the entries of global metadata they set, in the order written
 * @param ending how they end
 * @param code the code of the reason they fail or skip with, if any
 * @param message the message of the error they terminate abnormally with, if any
 */
public record ScriptRow(
    String step,
    String attempt,
    Map<String, Object> contributes,
    Emitted emits,
    String emitMessage,
    Map<String, Object> setsGlobalMetadata,
    ScriptedEnding ending,
    String code,
    String message) {}
