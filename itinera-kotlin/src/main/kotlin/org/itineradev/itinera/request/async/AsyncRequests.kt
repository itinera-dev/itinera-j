package org.itineradev.itinera.request.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.Handle
import org.itineradev.itinera.engine.requestOf
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepReporterKind
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.workflow.Async

/**
 * An asynchronous step's reporter, for its own events and for setting global metadata.
 *
 * @sample org.itineradev.itinera.request.async.samples.asyncRequests
 */
@ExperimentalItineraApi
public fun Requests.asyncStepReporter(): Request<StepReporterKind<Async>, AsyncStepReporter> =
    requestOf(Handle.ASYNC_STEP_REPORTER, Answer.ITSELF)
