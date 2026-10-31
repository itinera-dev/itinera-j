package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.ReporterFailure
import org.itineradev.itinera.conformance.trace.Events
import org.itineradev.itinera.event.EventKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RecordersTest {
    @Test
    fun `a reporter keeps the event it fails on, and remembers that it failed`() {
        val recorders =
            Recorders(
                mapOf("audit" to ReporterFailure.OnEvent(EventKind.ATTEMPT_STARTED, "disk full"))
            )
        val audit = recorders.make("audit")
        val events = Events()
        audit.report(events.started())
        val failure =
            assertThrows<ScriptedReporterFailure> { audit.report(events.attempt("charge")) }
        assertEquals("disk full", failure.message)
        assertEquals(
            listOf(EventKind.JOURNEY_STARTED, EventKind.ATTEMPT_STARTED),
            audit.events.map { it.kind },
        )
        assertTrue(audit.failed)
    }

    @Test
    fun `a reporter that fails on the first event fails on no other`() {
        val audit = Recorders(mapOf("audit" to ReporterFailure.OnFirstEvent)).make("audit")
        val events = Events()
        assertThrows<ScriptedReporterFailure> { audit.report(events.started()) }
        audit.report(events.attempt("charge"))
        assertEquals(2, audit.events.size)
    }

    @Test
    fun `an entry of global metadata records the kind of the last event before it`() {
        val audit = Recorders(emptyMap()).make("audit")
        audit.setGlobalMetadata("journey_id", "order-1")
        audit.report(Events().started())
        audit.setGlobalMetadata("tenant", "acme")
        assertEquals(
            listOf(
                Entry("journey_id", "order-1", null),
                Entry("tenant", "acme", EventKind.JOURNEY_STARTED),
            ),
            audit.entries,
        )
        assertFalse(audit.failed)
    }

    @Test
    fun `each reporter made under a name is kept in the order made`() {
        val recorders = Recorders(emptyMap())
        val first = recorders.make("audit")
        val second = recorders.make("audit")
        assertEquals(listOf(first, second), recorders.made("audit"))
        assertEquals(emptyList<ScenarioReporter>(), recorders.made("metrics"))
    }
}
