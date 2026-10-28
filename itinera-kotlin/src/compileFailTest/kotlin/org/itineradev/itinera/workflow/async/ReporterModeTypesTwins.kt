package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.WorkflowReporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.report.async.asyncWorkflowReporter
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** What [ReporterModeTypesTest] refuses, written so that it compiles. */
internal object ReporterModeTypesTwins {
    class Shop

    val printer = WorkflowReporter.of { Reporter {} }
    val asyncPrinter = asyncWorkflowReporter { AsyncReporter {} }

    val shop = WorkflowDescriptor.builder(Shop::class.java, "shop").reporter(printer)
    val asyncShop = asyncBuilder<Shop>("shop").reporter(asyncPrinter)
}
