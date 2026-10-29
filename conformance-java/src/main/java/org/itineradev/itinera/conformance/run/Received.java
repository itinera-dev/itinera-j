package org.itineradev.itinera.conformance.run;

import java.util.Map;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.step.StepReporter;

/**
 * What a scripted step was given for one attempt: its inputs, absent as {@code null}, and its
 * services, by key, its contributor and its step reporter, with how many steps were built from its
 * script before it, which plays the attempts' rows in turn, across journeys, as its step mock does,
 * and, for a rescue step, what it received of the step it rescues, by what it requested.
 *
 * @param inputs its inputs
 * @param services its services
 * @param contributor its contributor
 * @param reporter its step reporter
 * @param builtBefore how many steps were built from its script before it
 * @param ofRescuedStep what a rescue step received of the step it rescues, absent as {@code null}
 */
public record Received(
    Map<String, Object> inputs,
    Map<String, Object> services,
    Contributor contributor,
    StepReporter reporter,
    int builtBefore,
    Map<HookRequest, Object> ofRescuedStep) {}
