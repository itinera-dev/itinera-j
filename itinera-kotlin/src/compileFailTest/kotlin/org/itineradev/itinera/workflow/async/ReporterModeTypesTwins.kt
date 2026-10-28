package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.ReporterFactory
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.report.async.AsyncReporterFactory
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** What [ReporterModeTypesTest] refuses, written so that it compiles. */
internal object ReporterModeTypesTwins {
    class Shop

    val printer = ReporterFactory { Reporter {} }
    val asyncPrinter = AsyncReporterFactory { AsyncReporter {} }

    val shop = WorkflowDescriptor.builder(Shop::class.java, "shop").reporter(printer)
    val asyncShop = asyncBuilder<Shop>("shop").reporter(asyncPrinter)
}
