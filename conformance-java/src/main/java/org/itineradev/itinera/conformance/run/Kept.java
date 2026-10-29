package org.itineradev.itinera.conformance.run;

import java.util.List;
import java.util.Map;
import org.itineradev.itinera.event.Event;

/**
 * What the recording reporter of test support kept.
 *
 * @param events every event it received, in order
 * @param globalMetadata the latest value of each entry of global metadata it received
 * @param path its path
 */
public record Kept(List<Event> events, Map<String, Object> globalMetadata, List<String> path) {}
