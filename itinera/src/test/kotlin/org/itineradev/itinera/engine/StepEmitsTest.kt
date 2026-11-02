@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import java.time.Instant
import java.util.concurrent.CountDownLatch
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.reporting
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.StepError
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.event.StepWarning
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class StepEmitsTest {
    @ParameterizedTest
    @FieldSource("emits")
    fun `each emit delivers its own event, naming the step, the attempt and the message`(
        emit: StepReporter.(String) -> Unit,
        kind: EventKind,
        made: Made,
    ) {
        val log = Log()
        ran(
            journeyOf(reporting("charge", { reporter -> emitted(reporter, emit) }))
                .reporter(log)
                .create()
        )
        val found = log.events.single { event -> isOf(event, kind) }
        assertEquals(
            made(
                found.sequence,
                found.timestamp,
                found.journeyId,
                found.workflow,
                charge,
                Attempt.of(1),
                "noted",
                null,
            ),
            found,
        )
    }

    @Test
    fun `an event carries a copy of its data made when it is emitted`() {
        val log = Log()
        val charging =
            reporting(
                "charge",
                { reporter ->
                    val data = mutableMapOf("ms" to 1200L)
                    reporter.warning("gateway slow", data)
                    data["ms"] = 0L
                    Outcome.success()
                },
            )
        ran(journeyOf(charging).reporter(log).create())
        assertEquals(mapOf("ms" to 1200L), log.events.filterIsInstance<StepWarning>().single().data)
    }

    @Test
    fun `data that is not a value aborts the journey, and the event is not delivered`() {
        val log = Log()
        val charging =
            reporting(
                "charge",
                { reporter -> emitted(reporter) { message -> info(message, Thread()) } },
            )
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            NotAValue.InEvent(StepName.of("charge"), ByStep, EventKind.STEP_INFO, null),
            result.abort?.details,
        )
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    @Test
    fun `a reporter that fails on an event ends the step at the emit, even when the step catches it`() {
        val log = Log()
        val charging =
            reporting(
                "charge",
                { reporter ->
                    runCatching { reporter.info("first") }
                    runCatching { reporter.info("second") }
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).reporter(FailingOnInfo).reporter(log).create())
        assertEquals(ReporterFailed(StepName.of("charge"), "info refused"), result.abort?.details)
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    @Test
    fun `a reporter that fails on an event a step's thread emits as the attempt ends still aborts the journey`() {
        val log = Log()
        val failing = FailingLate()
        val charging = reporting("charge", { reporter -> leftEmitting(reporter, failing.entered) })
        val result = ran(journeyOf(charging).reporter(failing).reporter(log).create())
        assertEquals(ReporterFailed(StepName.of("charge"), "late refused"), result.abort?.details)
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    @Test
    fun `a step reporter used after its attempt throws and reports nothing`() {
        val log = Log()
        var kept: StepReporter? = null
        val charging =
            reporting("charge", { reporter -> Outcome.success().also { kept = reporter } })
        ran(journeyOf(charging).reporter(log).create())
        val events = log.events.size
        assertThrows<IllegalStateException> { kept?.info("stale") }
        assertThrows<IllegalStateException> { kept?.setGlobalMetadata("stale", 1L) }
        assertEquals(events, log.events.size)
        assertEquals(listOf("journey_id"), log.metadata.map(Pair<String, Any?>::first))
    }

    companion object {
        private val charge = StepName.of("charge")

        @JvmField
        val emits: List<Arguments> =
            listOf(
                argumentSet("info", info, EventKind.STEP_INFO, ::StepInfo as Made),
                argumentSet("warning", warning, EventKind.STEP_WARNING, ::StepWarning as Made),
                argumentSet("error", error, EventKind.STEP_ERROR, ::StepError as Made),
            )
    }
}

private fun isOf(event: Event, kind: EventKind): Boolean = event.kind == kind

/** Makes one step event from its fields. */
private typealias Made =
    (Long, Instant, JourneyId, WorkflowName, StepName, Attempt, String, Any?) -> Event

private val info: StepReporter.(String) -> Unit = StepReporter::info
private val warning: StepReporter.(String) -> Unit = StepReporter::warning
private val error: StepReporter.(String) -> Unit = StepReporter::error

private fun emitted(reporter: StepReporter, emit: StepReporter.(String) -> Unit): Outcome {
    reporter.emit("noted")
    return Outcome.success()
}

/** Starts a thread that emits, and returns once a reporter is delivering what it emitted. */
private fun leftEmitting(reporter: StepReporter, entered: CountDownLatch): Outcome {
    Thread { runCatching { reporter.info("late") } }.start()
    entered.await()
    return Outcome.success()
}

/** Fails on `step_info`, once the step that emitted it has had time to return. */
private class FailingLate : Reporter {
    val entered = CountDownLatch(1)

    override fun report(event: Event) {
        if (event.kind == EventKind.STEP_INFO) {
            entered.countDown()
            Thread.sleep(RETURNING_MS)
            error("late refused")
        }
    }

    override fun setGlobalMetadata(key: String, value: Any?) = Unit
}

private const val RETURNING_MS = 200L

private object FailingOnInfo : Reporter {
    override fun report(event: Event) {
        check(event.kind != EventKind.STEP_INFO) { "info refused" }
    }

    override fun setGlobalMetadata(key: String, value: Any?) = Unit
}
