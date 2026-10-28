@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.step.async.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.async.AsyncStepReporter

suspend fun asyncReporters(reporter: AsyncStepReporter) {
    reporter.info("shipping")
    reporter.info("shipping", mapOf("parcel" to "7"))
    reporter.warning("the carrier is late")
    reporter.warning("the carrier is late", mapOf("hours" to 2L))
    reporter.error("the parcel was lost")
    reporter.error("the parcel was lost", mapOf("parcel" to "7"))
    reporter.setGlobalMetadata("warehouse", "north")
}
