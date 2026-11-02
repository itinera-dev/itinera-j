package org.itineradev.itinera.request.async.samples

import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.async.asyncStep

fun asyncRequests(): StepBuilder<Async> {
    val reporter = Requests.asyncStepReporter()
    return asyncStep("ship") { got ->
            val events = got[reporter]
            AsyncStepRun {
                events.info("shipping")
                Outcome.success()
            }
        }
        .request(reporter)
}
