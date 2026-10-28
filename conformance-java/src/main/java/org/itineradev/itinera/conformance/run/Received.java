package org.itineradev.itinera.conformance.run;

import java.util.Map;
import org.itineradev.itinera.journey.Contributor;

/**
 * What a scripted step was given for one attempt: its inputs, absent as {@code null}, and its
 * services, by key, and its contributor, with how many steps were built from its script before it,
 * which plays the attempts' rows in turn, across journeys, as its step mock does.
 *
 * @param inputs its inputs
 * @param services its services
 * @param contributor its contributor
 * @param builtBefore how many steps were built from its script before it
 */
public record Received(
    Map<String, Object> inputs,
    Map<String, Object> services,
    Contributor contributor,
    int builtBefore) {}
