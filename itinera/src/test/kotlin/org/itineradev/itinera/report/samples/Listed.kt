package org.itineradev.itinera.report.samples

import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.samples.Orders
import org.itineradev.itinera.workflow.samples.steps

fun listed(lines: MutableList<String>): WorkflowDescriptor<Orders, Sync> =
    WorkflowDescriptor.builder(Orders::class.java, "orders")
        .step(steps())
        .reporter { journeyId -> Reporter { event -> lines += "$journeyId ${event.kind}" } }
        .build()
