@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.instance.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.report.DebugReporter
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.samples.Orders
import org.itineradev.itinera.workflow.samples.building
import org.itineradev.itinera.workflow.samples.ids

fun instances(): WorkflowInstance<Sync> {
    val instance =
        InstanceBuilder.of(ids(), Orders("corner"))
            .data("order", "42")
            .initialData(mapOf("price" to 40L, "discount" to 5L))
            .reporter(DebugReporter())
            .reportersGlobalMetadata("tenant", "acme")
            .create()
    check(instance.journeyId.toString() == "corner-42")
    return instance
}

fun fromKotlin(): WorkflowInstance<Sync> = building().instance(Orders("corner")).create()
