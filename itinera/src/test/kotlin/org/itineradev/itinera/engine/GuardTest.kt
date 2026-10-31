package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.packing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.report.Dispatcher
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.step.StepName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class GuardTest {
    @Test
    fun `a reporter that fails aborts the journey with reporter failed, naming the step it failed in`() {
        val error = IllegalStateException("disk full")
        val result = ran(packing().reporter(FailingOn(EventKind.STEP_SUCCEEDED, error)).create())
        assertEquals(ReporterFailed(StepName.of("pack"), "disk full"), result.abort?.details)
        assertSame(error, result.abort?.error)
    }

    @Test
    fun `the reporters after one that failed get only journey_aborted, and it gets nothing more`() {
        val before = Log()
        val failing = FailingOn(EventKind.ATTEMPT_STARTED, IllegalStateException("full"))
        val after = Log()
        ran(packing().reporter(before).reporter(failing).reporter(after).create())
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), before.kinds)
        assertEquals(listOf("journey_started", "journey_aborted"), after.kinds)
        assertEquals(listOf("journey_started"), failing.received)
    }

    @Test
    fun `the rules hold for a dispatcher that goes on delivering after a reporter fails`() {
        val failing = FailingOn(EventKind.ATTEMPT_STARTED, IllegalStateException("full"))
        val after = Log()
        val result =
            ran(
                packing().reporter(failing).reporter(after).create(),
                DispatcherFactory(::Careless),
            )
        assertEquals(listOf("journey_started", "journey_aborted"), after.kinds)
        assertEquals(listOf("journey_started"), failing.received)
        assertEquals(ReporterFailed(StepName.of("pack"), "full"), result.abort?.details)
    }

    @Test
    fun `a failure while journey_aborted is delivered is ignored, and delivery goes on`() {
        val failing = FailingOn(EventKind.JOURNEY_ABORTED, IllegalStateException("late"))
        val after = Log()
        val result =
            ran(
                packing()
                    .reporter(FailingOn(EventKind.ATTEMPT_STARTED, IllegalStateException("first")))
                    .reporter(failing)
                    .reporter(after)
                    .create()
            )
        assertEquals("first", result.abort?.errorMessage)
        assertEquals("journey_aborted", after.kinds.last())
    }

    @Test
    fun `a dispatcher that throws while it dispatches aborts the journey with reporter failed`() {
        val error = IllegalStateException("queue full")
        val result = ran(packing().reporter(Log()).create(), DispatcherFactory { Refusing(error) })
        assertEquals(ReporterFailed(StepName.of("pack"), "queue full"), result.abort?.details)
        assertSame(error, result.abort?.error)
    }

    @Test
    fun `an Error from a reporter on journey_aborted is never caught`() {
        assertThrows<StackOverflowError> {
            ran(
                packing()
                    .reporter(FailingOn(EventKind.ATTEMPT_STARTED, IllegalStateException("full")))
                    .reporter(FailingOn(EventKind.JOURNEY_ABORTED, StackOverflowError()))
                    .create()
            )
        }
    }

    @Test
    fun `an Error from a reporter is never caught`() {
        assertThrows<StackOverflowError> {
            ran(
                packing()
                    .reporter(FailingOn(EventKind.JOURNEY_STARTED, StackOverflowError()))
                    .create()
            )
        }
    }
}

/** Throws [thrown] on the first event of [kind], and keeps the kinds it received before. */
@OptIn(ExperimentalItineraApi::class)
private class FailingOn(private val kind: EventKind, private val thrown: Throwable) : Reporter {
    val received = mutableListOf<String>()

    override fun report(event: Event) {
        if (event.kind == kind) throw thrown
        received += event.kind.toString()
    }
}

/** Delivers every event to every reporter, ignoring what each throws. */
@OptIn(ExperimentalItineraApi::class)
private class Careless : Dispatcher {
    private val reporters = mutableListOf<Reporter>()

    override fun add(reporter: Reporter) {
        reporters += reporter
    }

    override fun dispatch(event: Event) {
        for (reporter in reporters) runCatching { reporter.report(event) }
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        for (reporter in reporters) runCatching { reporter.setGlobalMetadata(key, value) }
    }
}

/** Throws [thrown] when asked to dispatch `attempt_started`, as a dispatcher must not. */
@OptIn(ExperimentalItineraApi::class)
private class Refusing(private val thrown: Throwable) : Dispatcher {
    override fun add(reporter: Reporter) = Unit

    override fun dispatch(event: Event) {
        if (event.kind == EventKind.ATTEMPT_STARTED) throw thrown
    }

    override fun setGlobalMetadata(key: String, value: Any?) = Unit
}
