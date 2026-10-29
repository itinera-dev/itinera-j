package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.report.testing.complete
import org.itineradev.itinera.step.testing.samples.asyncStepEventReporter
import org.itineradev.itinera.step.testing.samples.failingAsyncStepEventReporter
import org.itineradev.itinera.step.testing.samples.failingStepEventReporter
import org.itineradev.itinera.step.testing.samples.stepEventReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class StepEventReporterTest {
    @Test
    fun `a step event reporter keeps every event emitted, in order`() {
        assertEquals(
            listOf("charging", "the gateway is slow"),
            stepEventReporter().map { it.message },
        )
    }

    @Test
    fun `an asynchronous step event reporter keeps every event emitted`() {
        assertEquals(1, complete { asyncStepEventReporter() }.size)
    }

    @Test
    fun `a failing step event reporter interrupts every call and keeps nothing`() {
        assertTrue(failingStepEventReporter())
    }

    @Test
    fun `setting journey_id interrupts that call and every later one`() {
        val recorder = StepEventReporter()
        assertThrows<RuntimeException> { recorder.reporter.setGlobalMetadata("journey_id", "o-1") }
        assertThrows<RuntimeException> { recorder.reporter.info("charging") }
        assertTrue(recorder.emitted.isEmpty())
        assertTrue(recorder.globalMetadata.isEmpty())
    }

    @Test
    fun `a failing asynchronous step event reporter interrupts every call and keeps nothing`() {
        assertTrue(complete { failingAsyncStepEventReporter() })
    }

    @Test
    fun `setting journey_id on an asynchronous step event reporter interrupts that call and every later one`() {
        val recorder = AsyncStepEventReporter()
        complete {
            assertTrue(
                runCatching { recorder.reporter.setGlobalMetadata("journey_id", "o-1") }.isFailure
            )
            assertTrue(runCatching { recorder.reporter.info("charging") }.isFailure)
        }
        assertTrue(recorder.emitted.isEmpty())
    }
}
