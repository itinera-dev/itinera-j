package org.itineradev.itinera.step.samples

import org.itineradev.itinera.step.StepReporter

fun reporters(reporter: StepReporter) {
    reporter.info("charging")
    reporter.info("charging", mapOf("amount" to 40L))
    reporter.warning("the gateway is slow")
    reporter.warning("the gateway is slow", mapOf("seconds" to 3L))
    reporter.error("the card was declined")
    reporter.error("the card was declined", mapOf("code" to "51"))
    reporter.setGlobalMetadata("tenant", "corner")
}
