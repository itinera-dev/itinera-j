package org.itineradev.itinera.conformance.run;

/**
 * One journey the scenario ran.
 *
 * @param journeyId the ID of its instance, or {@code null} when none was created
 * @param ended how it ended
 */
public record Journey(String journeyId, Ended ended) {}
