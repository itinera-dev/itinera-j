@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.report.samples

import java.time.Instant
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.DebugReporter
import org.itineradev.itinera.report.DefaultDispatcherFactory
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.workflow.WorkflowName

fun started(): Event =
    JourneyStarted(
        1L,
        Instant.parse("2026-10-27T18:00:00Z"),
        JourneyId.of("order-42"),
        WorkflowName.of("orders"),
        setOf("amount"),
    )

fun printer(): Reporter = Reporter { event -> println("${event.journeyId} ${event.kind}") }

/** Writes each event with the tenant the journey's global metadata names. */
class Tenants(private val lines: MutableList<String>) : Reporter {
    private var tenant: Any? = null

    override fun setGlobalMetadata(key: String, value: Any?) {
        if (key == "tenant") tenant = value
    }

    override fun report(event: Event) {
        lines += "$tenant: ${event.kind}"
    }
}

fun tenants(): List<String> {
    val lines = mutableListOf<String>()
    val reporter = Tenants(lines)
    reporter.setGlobalMetadata("tenant", "acme")
    reporter.report(started())
    return lines
}

fun dispatching(): List<String> {
    val lines = mutableListOf<String>()
    val dispatcher = DefaultDispatcherFactory().create()
    dispatcher.add(Reporter { event -> lines += "first: ${event.kind}" })
    dispatcher.add(Tenants(lines))
    dispatcher.setGlobalMetadata("tenant", "acme")
    dispatcher.dispatch(started())
    return lines
}

fun debugging() {
    DebugReporter().report(started())
}
