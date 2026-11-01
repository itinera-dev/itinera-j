@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.report.async.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncStep
import org.itineradev.itinera.workflow.async.samples.AsyncOrders

fun asyncListed(lines: MutableList<String>): WorkflowDescriptor<AsyncOrders, Async> =
    asyncBuilder<AsyncOrders>("orders")
        .step(asyncStep("charge") { AsyncStepRun { Outcome.success() } }.build())
        .reporter { journeyId -> AsyncReporter { event -> lines += "$journeyId ${event.kind}" } }
        .build()
