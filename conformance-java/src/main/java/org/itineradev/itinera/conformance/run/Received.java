package org.itineradev.itinera.conformance.run;

import java.util.Map;

/**
 * What a scripted step was given for one attempt: its inputs, absent as {@code null}, and its
 * services, by key.
 *
 * @param inputs its inputs
 * @param services its services
 */
public record Received(Map<String, Object> inputs, Map<String, Object> services) {}
