@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.instance.async.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.async.AsyncInstanceBuilder
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.report.async.AsyncDebugReporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.itineradev.itinera.workflow.async.samples.asyncBuilding

fun asyncInstances(): WorkflowInstance<Async> {
    val instance =
        AsyncInstanceBuilder.of(asyncBuilding(), AsyncOrders("corner"))
            .data("order", "42")
            .initialData(mapOf("price" to 40L, "discount" to 5L))
            .reporter(AsyncDebugReporter())
            .reportersGlobalMetadata("tenant", "acme")
            .create()
    check(instance.journeyId.toString() == "corner-1")
    return instance
}

fun asyncPerJourney(lines: MutableList<String>): WorkflowInstance<Async> =
    asyncBuilding()
        .instance(AsyncOrders("corner"))
        .reporterFor { journeyId ->
            AsyncReporter { event -> lines += "$journeyId: ${event.kind}" }
        }
        .create()
