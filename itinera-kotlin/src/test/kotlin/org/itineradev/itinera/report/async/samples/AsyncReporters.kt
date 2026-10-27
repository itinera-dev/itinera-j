@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.report.async.samples

import java.time.Instant
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.async.AsyncDebugReporter
import org.itineradev.itinera.report.async.AsyncDefaultDispatcherFactory
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.workflow.WorkflowName

fun started(): Event =
    JourneyStarted(
        1L,
        Instant.parse("2026-10-27T18:00:00Z"),
        JourneyId.of("order-42"),
        WorkflowName.of("orders"),
        setOf("amount"),
    )

fun asyncPrinter(): AsyncReporter = AsyncReporter { event ->
    println("${event.journeyId} ${event.kind}")
}

/** Writes each event with the tenant the journey's global metadata names. */
class AsyncTenants(private val lines: MutableList<String>) : AsyncReporter {
    private var tenant: Any? = null

    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        if (key == "tenant") tenant = value
    }

    override suspend fun report(event: Event) {
        lines += "$tenant: ${event.kind}"
    }
}

suspend fun asyncTenants(): List<String> {
    val lines = mutableListOf<String>()
    val reporter = AsyncTenants(lines)
    reporter.setGlobalMetadata("tenant", "acme")
    reporter.report(started())
    return lines
}

suspend fun asyncDispatching(): List<String> {
    val lines = mutableListOf<String>()
    val dispatcher = AsyncDefaultDispatcherFactory().create()
    dispatcher.add(AsyncReporter { event -> lines += "first: ${event.kind}" })
    dispatcher.add(AsyncTenants(lines))
    dispatcher.setGlobalMetadata("tenant", "acme")
    dispatcher.dispatch(started())
    return lines
}

suspend fun asyncDebugging() {
    AsyncDebugReporter().report(started())
}
