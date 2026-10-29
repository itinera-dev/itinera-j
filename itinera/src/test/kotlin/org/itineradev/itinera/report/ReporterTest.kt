package org.itineradev.itinera.report

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.report.Fixtures.started
import org.itineradev.itinera.report.samples.debugging
import org.itineradev.itinera.report.samples.listed
import org.itineradev.itinera.report.samples.perJourney
import org.itineradev.itinera.report.samples.printer
import org.itineradev.itinera.report.samples.tenants
import org.itineradev.itinera.workflow.samples.Orders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ReporterTest {
    @Test
    fun `a reporter accepts every entry of global metadata unless it overrides it`() {
        assertDoesNotThrow { Reporter {}.setGlobalMetadata("tenant", listOf("acme")) }
    }

    @Test
    fun `a debug reporter prints each event as its record prints, on a line of its own`() {
        val printed = ByteArrayOutputStream()
        DebugReporter(PrintStream(printed, true, Charsets.UTF_8)).report(started)
        assertEquals("$started${System.lineSeparator()}", printed.toString(Charsets.UTF_8))
    }

    @Test
    fun `the sample keeps the latest tenant for the events that follow`() {
        assertEquals(listOf("acme: journey_started"), tenants())
    }

    @Test
    fun `the printer and debugging samples run without throwing`() {
        assertDoesNotThrow {
            printer().report(started)
            debugging()
        }
    }

    @Test
    fun `a reporter factory makes a reporter for each journey, given its ID`() {
        val lines = mutableListOf<String>()
        val instance = perJourney(lines)
        LocalExecutor().run(instance)
        assertEquals("${instance.journeyId} journey_started", lines.first())
    }

    @Test
    fun `a reporter the workflow lists is made for each instance, given its journey ID`() {
        val lines = mutableListOf<String>()
        val instance = listed(lines).instance(Orders("corner")).create()
        LocalExecutor().run(instance)
        assertEquals("${instance.journeyId} journey_started", lines.first())
    }
}
