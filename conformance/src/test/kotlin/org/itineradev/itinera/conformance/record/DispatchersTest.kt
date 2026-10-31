package org.itineradev.itinera.conformance.record

import java.time.Instant
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

internal typealias Mode = (DispatcherScript) -> Scripted

internal class DispatchersTest {
    @ParameterizedTest
    @FieldSource("modes")
    fun `a scripted dispatcher delivers to the reporter it holds and to those added`(mode: Mode) {
        val scripted = mode(DispatcherScript.Delivers)
        scripted.add()
        scripted.setGlobalMetadata("tenant", "acme")
        scripted.dispatch(started)
        assertEquals(listOf(started), scripted.held)
        assertEquals(mapOf("tenant" to "acme"), scripted.heldMetadata)
        assertEquals(listOf(started), scripted.added)
    }

    @ParameterizedTest
    @FieldSource("modes")
    fun `a dispatcher that ignores added reporters delivers only to the one it holds`(mode: Mode) {
        val scripted = mode(DispatcherScript.IgnoresAddedReporters)
        scripted.add()
        scripted.dispatch(started)
        assertEquals(listOf(started), scripted.held)
        assertEquals(emptyList<Event>(), scripted.added)
    }

    @ParameterizedTest
    @FieldSource("modes")
    fun `a dispatcher that throws when a reporter is added adds none`(mode: Mode) {
        val scripted = mode(DispatcherScript.ThrowsWhenAdding)
        assertThrows<DispatcherFailure> { scripted.add() }
        scripted.dispatch(started)
        assertEquals(emptyList<Event>(), scripted.added)
    }

    @ParameterizedTest
    @FieldSource("modes")
    fun `a dispatcher that throws on one kind of event delivers every other`(mode: Mode) {
        val scripted = mode(DispatcherScript.ThrowsWhenDispatching(EventKind.JOURNEY_ABORTED))
        scripted.dispatch(started)
        assertThrows<DispatcherFailure> { scripted.dispatch(aborted) }
        assertEquals(listOf(started), scripted.held)
    }

    @Test
    fun `a failing factory throws when asked for a dispatcher`() {
        assertThrows<DispatcherFailure> { failingDispatchers.create() }
    }

    @Test
    fun `a failing asynchronous factory throws when asked for a dispatcher`() {
        assertThrows<DispatcherFailure> { complete { failingAsyncDispatchers.create() } }
    }

    private companion object {
        val timestamp: Instant = Instant.parse("2026-10-28T02:00:00Z")
        val journey: JourneyId = JourneyId.of("order-42")
        val workflow: WorkflowName = WorkflowName.of("orders")
        val started = JourneyStarted(1, timestamp, journey, workflow, setOf("amount"))
        val aborted = JourneyAborted(2, timestamp, journey, workflow, ReporterFailed(null, "full"))

        @JvmStatic
        val modes =
            listOf(
                argumentSet("a synchronous dispatcher", ::Synchronous as Mode),
                argumentSet("an asynchronous dispatcher", ::Asynchronous as Mode),
            )
    }
}
