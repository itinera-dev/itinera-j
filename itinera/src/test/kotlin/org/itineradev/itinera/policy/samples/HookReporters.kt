@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.HookReporter

fun hookReporters(reporter: HookReporter) {
    reporter.info("audited")
    reporter.info("audited", mapOf("amount" to 40L))
    reporter.warning("the amount is unusual")
    reporter.warning("the amount is unusual", mapOf("amount" to 4000L))
    reporter.error("the audit log is full")
    reporter.error("the audit log is full", mapOf("entries" to 10000L))
    reporter.setGlobalMetadata("audited", true)
}
