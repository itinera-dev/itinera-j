package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.policy.testing.samples.asyncHookEventReporter
import org.itineradev.itinera.policy.testing.samples.failingAsyncHookEventReporter
import org.itineradev.itinera.policy.testing.samples.failingHookEventReporter
import org.itineradev.itinera.policy.testing.samples.hookEventReporter
import org.itineradev.itinera.report.testing.complete
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HookEventReporterTest {
    @Test
    fun `a hook event reporter keeps every event emitted`() {
        assertEquals(1, hookEventReporter().size)
    }

    @Test
    fun `an asynchronous hook event reporter keeps every event emitted`() {
        assertEquals(1, complete { asyncHookEventReporter() }.size)
    }

    @Test
    fun `a failing hook event reporter interrupts every call and keeps nothing`() {
        assertTrue(failingHookEventReporter())
    }

    @Test
    fun `setting journey_id interrupts that call and every later one`() {
        val recorder = HookEventReporter()
        assertThrows<RuntimeException> { recorder.reporter.setGlobalMetadata("journey_id", "o-1") }
        assertThrows<RuntimeException> { recorder.reporter.info("audited") }
        assertTrue(recorder.emitted.isEmpty())
    }

    @Test
    fun `a failing asynchronous hook event reporter interrupts every call and keeps nothing`() {
        assertTrue(complete { failingAsyncHookEventReporter() })
    }

    @Test
    fun `setting journey_id on an asynchronous hook event reporter interrupts that call and every later one`() {
        val recorder = AsyncHookEventReporter()
        complete {
            assertTrue(
                runCatching { recorder.reporter.setGlobalMetadata("journey_id", "o-1") }.isFailure
            )
            assertTrue(runCatching { recorder.reporter.info("audited") }.isFailure)
        }
        assertTrue(recorder.emitted.isEmpty())
    }
}
