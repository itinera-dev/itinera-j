package org.itineradev.itinera.conformance.run;

/**
 * One step built for an attempt: the step itself, and what it received.
 *
 * @param step the step
 * @param received what it received
 */
public record Built(Object step, Received received) {}
