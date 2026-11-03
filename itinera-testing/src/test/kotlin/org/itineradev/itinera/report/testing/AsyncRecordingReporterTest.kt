package org.itineradev.itinera.report.testing

import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.report.testing.samples.asyncRecording
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncRecordingReporterTest {
    @Test
    fun `an asynchronous recording reporter keeps what a synchronous one keeps`() {
        val recorder = complete { asyncRecording() }
        assertEquals(
            listOf(
                EventKind.JOURNEY_STARTED,
                EventKind.STEP_SUCCEEDED,
                EventKind.JOURNEY_SUCCEEDED,
            ),
            recorder.events.map { it.kind },
        )
        assertEquals(mapOf("journey_id" to "order-42"), recorder.globalMetadata)
        assertEquals(listOf("charge attempt 1 succeeded", "journey succeeded"), recorder.path)
    }
}
