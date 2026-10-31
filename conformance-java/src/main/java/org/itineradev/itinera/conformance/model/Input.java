package org.itineradev.itinera.conformance.model;

/**
 * A required or optional input a step requests, of a type of the cases.
 *
 * @param key the key it is read under
 * @param type its type
 * @param optional whether the step runs without it
 */
public record Input(String key, ValueType type, boolean optional) {}
