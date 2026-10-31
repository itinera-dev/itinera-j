package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.workflow.Mode

/**
 * A service provider of journey scope in a workflow of mode [M], as the kind of part that requests:
 * the journey ID, data from the workflow and the provider reporter of its mode. It is called once,
 * for whichever step needs it first, so it never requests the step's name or the attempt.
 *
 * @param M the mode of the workflows the provider can join.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi
public interface JourneyProviderKind<M : Mode> :
    JourneyIdKind, WorkflowDataKind, ProviderReporterKind<M>

/**
 * A service provider of attempt scope in a workflow of mode [M], as the kind of part that requests:
 * what one of journey scope may, and the name of the step being built and its attempt.
 *
 * @param M the mode of the workflows the provider can join.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi
public interface AttemptProviderKind<M : Mode> : JourneyProviderKind<M>, StepNameKind, AttemptKind

/**
 * The kinds of part of mode [M] that may request the provider reporter of that mode: service
 * providers.
 *
 * @param M the mode of the reporter they may request.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi public interface ProviderReporterKind<M : Mode>
