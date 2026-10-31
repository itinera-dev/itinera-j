package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.event.EventKind;

/**
 * An entry of global metadata a reporter received, and the kind of the last event before it, or
 * {@code null} when none came before.
 */
public record Entry(String key, Object value, EventKind after) {}
