package org.itineradev.itinera.report

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.report.Fixtures.aborted
import org.itineradev.itinera.report.Fixtures.received
import org.itineradev.itinera.report.Fixtures.started
import org.itineradev.itinera.report.samples.dispatching
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class DefaultDispatcherTest {
    private val log = mutableListOf<String>()

    @Test
    fun `reporters receive each event in the order they were added`() {
        recordingAAndB().dispatch(started)
        assertEquals(listOf(received("a", started), received("b", started)), log)
    }

    @Test
    fun `reporters receive each entry of global metadata in the order they were added`() {
        recordingAAndB().setGlobalMetadata("tenant", "acme")
        assertEquals(listOf(received("a", "tenant", "acme"), received("b", "tenant", "acme")), log)
    }

    @Test
    fun `with no reporter an event goes nowhere`() {
        assertDoesNotThrow { DefaultDispatcher().dispatch(started) }
    }

    @Test
    fun `a reporter that throws stops the delivery of the event, and the dispatcher throws it`() {
        val full = IllegalStateException("full")
        val dispatcher = failingBetweenAAndB(full)
        assertSame(full, assertThrows<IllegalStateException> { dispatcher.dispatch(started) })
        assertEquals(listOf(received("a", started)), log)
    }

    @Test
    fun `a reporter that throws stops the delivery of an entry of global metadata`() {
        val full = IllegalStateException("full")
        val dispatcher = failingBetweenAAndB(full)
        assertThrows<IllegalStateException> { dispatcher.setGlobalMetadata("tenant", "acme") }
        assertEquals(listOf(received("a", "tenant", "acme")), log)
    }

    @Test
    fun `journey_aborted reaches every reporter, and the dispatcher throws the first failure`() {
        val first = IllegalStateException("first")
        val dispatcher = failingBetweenAAndB(first)
        dispatcher.add(Failing(IllegalStateException("second")))
        assertSame(first, assertThrows<IllegalStateException> { dispatcher.dispatch(aborted) })
        assertEquals(listOf(received("a", aborted), received("b", aborted)), log)
    }

    @Test
    fun `an Error from a reporter is never caught, even on journey_aborted`() {
        val dispatcher = failingBetweenAAndB(OutOfMemoryError("full"))
        assertThrows<OutOfMemoryError> { dispatcher.dispatch(aborted) }
        assertEquals(listOf(received("a", aborted)), log)
    }

    @Test
    fun `the factory creates a dispatcher of its own for each journey`() {
        val factory = DefaultDispatcherFactory()
        assertNotSame(factory.create(), factory.create())
    }

    @Test
    fun `the sample sets global metadata, then dispatches`() {
        assertEquals(listOf("first: journey_started", "acme: journey_started"), dispatching())
    }

    private fun recordingAAndB(): DefaultDispatcher =
        DefaultDispatcher().apply {
            add(Recording("a", log))
            add(Recording("b", log))
        }

    private fun failingBetweenAAndB(thrown: Throwable): DefaultDispatcher =
        DefaultDispatcher().apply {
            add(Recording("a", log))
            add(Failing(thrown))
            add(Recording("b", log))
        }
}
