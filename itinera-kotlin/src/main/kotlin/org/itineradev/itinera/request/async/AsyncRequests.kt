package org.itineradev.itinera.request.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.Handle
import org.itineradev.itinera.engine.requestOf
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.request.HookReporterKind
import org.itineradev.itinera.request.ProviderReporterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepReporterKind
import org.itineradev.itinera.service.async.AsyncProviderReporter
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.workflow.Async

/**
 * An asynchronous step's reporter, for its own events and for setting global metadata.
 *
 * @sample org.itineradev.itinera.request.async.samples.asyncRequests
 */
public fun Requests.asyncStepReporter(): Request<StepReporterKind<Async>, AsyncStepReporter> =
    requestOf(Handle.ASYNC_STEP_REPORTER, Answer.ITSELF)

/**
 * An asynchronous hook's reporter, for its own events and for setting global metadata.
 *
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
public fun Requests.asyncHookReporter(): Request<HookReporterKind<Async>, AsyncHookReporter> =
    requestOf(Handle.ASYNC_HOOK_REPORTER, Answer.ITSELF)

/**
 * An asynchronous service provider's reporter, for its own events and for setting global metadata.
 *
 * @sample org.itineradev.itinera.service.async.samples.asyncProviders
 */
@ExperimentalItineraApi
public fun Requests.asyncProviderReporter():
    Request<ProviderReporterKind<Async>, AsyncProviderReporter> =
    requestOf(Handle.ASYNC_PROVIDER_REPORTER, Answer.ITSELF)
