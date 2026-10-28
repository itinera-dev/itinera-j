package org.itineradev.itinera.request.async

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.async.asyncStep

/** What [RequestModeTypesTest] refuses, written so that it compiles. */
internal object RequestModeTypesTwins {
    fun read(got: Got<StepKind<Sync>>) = got[Requests.stepReporter()]

    fun readAsync(got: Got<StepKind<Async>>) = got[Requests.asyncStepReporter()]

    val ship = asyncStep("ship") { error("never built") }.request(Requests.asyncStepReporter())
}
