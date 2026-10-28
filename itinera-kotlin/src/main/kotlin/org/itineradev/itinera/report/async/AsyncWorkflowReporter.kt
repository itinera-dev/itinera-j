package org.itineradev.itinera.report.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Builders
import org.itineradev.itinera.report.WorkflowReporter
import org.itineradev.itinera.workflow.Async

/**
 * The reporter [factory] makes for each instance of an asynchronous workflow that lists it, with
 * the instance's journey ID, before the reporters the instance is given itself.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncListed
 */
@ExperimentalItineraApi
public fun asyncWorkflowReporter(factory: AsyncReporterFactory): WorkflowReporter<Async> =
    Builders.reporter(factory::create)
