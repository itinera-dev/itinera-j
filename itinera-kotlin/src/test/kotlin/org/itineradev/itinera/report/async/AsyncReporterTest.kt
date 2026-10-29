package org.itineradev.itinera.report.async

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.report.async.Fixtures.started
import org.itineradev.itinera.report.async.samples.asyncDebugging
import org.itineradev.itinera.report.async.samples.asyncListed
import org.itineradev.itinera.report.async.samples.asyncPrinter
import org.itineradev.itinera.report.async.samples.asyncTenants
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class AsyncReporterTest {
    @Test
    fun `a reporter accepts every entry of global metadata unless it overrides it`() {
        assertDoesNotThrow {
            complete { AsyncReporter {}.setGlobalMetadata("tenant", listOf("acme")) }
        }
    }

    @Test
    fun `a debug reporter prints each event as its record prints, on a line of its own`() {
        val printed = ByteArrayOutputStream()
        complete { AsyncDebugReporter(PrintStream(printed, true, Charsets.UTF_8)).report(started) }
        assertEquals("$started${System.lineSeparator()}", printed.toString(Charsets.UTF_8))
    }

    @Test
    fun `the sample keeps the latest tenant for the events that follow`() {
        assertEquals(listOf("acme: journey_started"), complete { asyncTenants() })
    }

    @Test
    fun `the printer and debugging samples run without throwing`() {
        assertDoesNotThrow {
            complete {
                asyncPrinter().report(started)
                asyncDebugging()
            }
        }
    }

    @Test
    fun `a reporter the workflow lists is made for each instance, given its journey ID`() {
        val lines = mutableListOf<String>()
        val instance = asyncListed(lines).instance(AsyncOrders("corner")).create()
        complete { AsyncLocalExecutor().run(instance) }
        assertEquals("${instance.journeyId} journey_started", lines.first())
    }
}
