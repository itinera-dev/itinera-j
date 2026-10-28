package org.itineradev.itinera.conformance.trace;

/**
 * A column of an event table and what an event writes in it, which is left empty when {@code null}.
 *
 * @param column the column
 * @param value what is written in it
 */
record Cell(String column, Object value) {}
