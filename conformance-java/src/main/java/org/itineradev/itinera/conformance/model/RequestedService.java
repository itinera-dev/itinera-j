package org.itineradev.itinera.conformance.model;

/**
 * A service a step or rescue step requests.
 *
 * @param key the key it is requested under
 * @param type its type
 */
public record RequestedService(String key, ServiceType type) {}
