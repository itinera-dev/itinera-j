@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.async.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.async.AsyncHookReporter

suspend fun asyncHookReporters(reporter: AsyncHookReporter) {
    reporter.info("audited")
    reporter.info("audited", mapOf("amount" to 40L))
    reporter.warning("the amount is unusual")
    reporter.warning("the amount is unusual", mapOf("amount" to 4000L))
    reporter.error("the audit log is full")
    reporter.error("the audit log is full", mapOf("entries" to 10000L))
    reporter.setGlobalMetadata("audited", true)
}
