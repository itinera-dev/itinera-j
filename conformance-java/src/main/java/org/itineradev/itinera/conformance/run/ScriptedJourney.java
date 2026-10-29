package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.executor.testing.ScriptedRun;

/**
 * A journey run with a script.
 *
 * @param journey the journey
 * @param run the scripted run's record, {@code null} when it never ran
 */
public record ScriptedJourney(Journey journey, ScriptedRun run) {}
