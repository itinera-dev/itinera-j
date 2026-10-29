package org.itineradev.itinera.service.async.samples

import org.itineradev.itinera.service.async.AsyncProviderReporter

suspend fun asyncProviderReporters(reporter: AsyncProviderReporter) {
    reporter.info("gateway opened")
    reporter.info("gateway opened", mapOf("region" to "eu"))
    reporter.warning("the gateway is slow")
    reporter.warning("the gateway is slow", mapOf("milliseconds" to 900L))
    reporter.error("the gateway refused the credentials")
    reporter.error("the gateway refused the credentials", mapOf("attempts" to 3L))
    reporter.setGlobalMetadata("gateway", "eu")
}
