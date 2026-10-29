package org.itineradev.itinera.conformance.model;

/**
 * What a rescue step requests of the step it rescues, written after some of its inputs.
 *
 * @param request what it requests
 * @param inputsBefore how many of its inputs were written before it
 */
public record RescuedStepRequest(HookRequest request, int inputsBefore) {}
