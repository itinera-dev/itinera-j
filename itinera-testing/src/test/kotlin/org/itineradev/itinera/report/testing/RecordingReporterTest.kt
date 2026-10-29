package org.itineradev.itinera.report.testing

import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.report.testing.samples.metadata
import org.itineradev.itinera.report.testing.samples.path
import org.itineradev.itinera.report.testing.samples.recording
import org.itineradev.itinera.report.testing.samples.succeeding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecordingReporterTest {
    @Test
    fun `a recording reporter keeps every event in the order it received them`() {
        assertEquals(
            listOf(
                EventKind.JOURNEY_STARTED,
                EventKind.STEP_SUCCEEDED,
                EventKind.JOURNEY_SUCCEEDED,
            ),
            recording(),
        )
    }

    @Test
    fun `global metadata keeps each key's latest value in the order the keys were first set`() {
        assertEquals(
            listOf("journey_id" to "order-42", "tenant" to "globex", "region" to "eu"),
            metadata().toList(),
        )
    }

    @Test
    fun `what a test read stays as it was while the journey goes on`() {
        val recorder = RecordingReporter()
        recorder.setGlobalMetadata("tenant", "acme")
        val events = recorder.events
        val metadata = recorder.globalMetadata
        val path = recorder.path
        succeeding().forEach(recorder::report)
        recorder.setGlobalMetadata("tenant", "globex")
        assertEquals(emptyList<Any>(), events)
        assertEquals(mapOf("tenant" to "acme"), metadata)
        assertEquals(emptyList<String>(), path)
    }

    @Test
    fun `a path lists the flow of the journey in the order it went`() {
        assertEquals(
            listOf(
                "charge attempt 1 failed: declined",
                "charge retrying",
                "charge attempt 2 failed: declined",
                "charge given up: retries exhausted",
                "journey failed at charge: declined",
            ),
            path(),
        )
    }

    @Test
    fun `a reporter that received nothing has an empty path`() {
        assertEquals(emptyList<String>(), RecordingReporter().path)
    }
}
