package org.itineradev.itinera.engine

import java.time.Instant
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.packing
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalItineraApi::class)
class EngineTest {
    @Test
    fun `a workflow whose steps succeed emits each attempt and succeeds with its initial data`() {
        val log = Log()
        val result =
            ran(
                journeyOf(succeeding("pack"), succeeding("ship"))
                    .data("order", "42")
                    .reporter(log)
                    .create()
            )
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_succeeded",
                "attempt_started",
                "step_succeeded",
                "journey_succeeded",
            ),
            log.kinds,
        )
        assertEquals(setOf("order"), (result.status as JourneyStatus.Succeeded).dataBag.keys)
    }

    @Test
    fun `events are numbered from 1 and stamped with the journey, the workflow and the clock's time`() {
        val log = Log()
        val instance = packing().reporter(log).create()
        ran(instance)
        assertEquals(listOf(1L, 2L, 3L, 4L), log.events.map(Event::sequence))
        assertEquals(setOf(instance.journeyId), log.events.map(Event::journeyId).toSet())
        assertEquals(setOf(WorkflowName.of("shop")), log.events.map(Event::workflow).toSet())
        assertEquals(
            setOf(Instant.parse("2026-10-28T09:00:00Z")),
            log.events.map(Event::timestamp).toSet(),
        )
    }

    @Test
    fun `journey_started lists the keys of the initial data`() {
        val log = Log()
        ran(packing().data("price", 40L).data("order", "42").reporter(log).create())
        assertEquals(setOf("price", "order"), (log.events.first() as JourneyStarted).initialKeys)
    }

    @Test
    fun `an abnormal termination with no retry budget gives the step up and fails the journey`() {
        val log = Log()
        val error = IllegalStateException("carrier down")
        val result = ran(journeyOf(playing("ship", error)).reporter(log).create())
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_abnormal_termination",
                "step_given_up",
                "journey_failed",
            ),
            log.kinds,
        )
        assertEquals("carrier down", (log.events[2] as StepAbnormalTermination).errorMessage)
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
        assertSame(error, result.failure?.error)
    }

    @Test
    fun `an abnormal termination is retried while the retry budget lasts`() {
        val log = Log()
        val result =
            ran(
                journeyOf(playing("ship", IllegalStateException("down"), null).retryBudget(1))
                    .reporter(log)
                    .create()
            )
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_abnormal_termination",
                "step_retrying",
                "attempt_started",
                "step_succeeded",
                "journey_succeeded",
            ),
            log.kinds,
        )
        assertEquals(2, (log.events[4] as AttemptStarted).attempt.number)
        assertTrue(result.isSucceeded)
    }

    @Test
    fun `a spent retry budget gives the step up with retries exhausted`() {
        val result =
            ran(
                journeyOf(
                        playing("ship", IllegalStateException("one"), IllegalStateException("two"))
                            .retryBudget(1)
                    )
                    .create()
            )
        assertEquals(FailureCause.RETRIES_EXHAUSTED, result.failure?.cause)
        assertEquals("two", result.failure?.errorMessage)
    }

    @Test
    fun `without retry on abnormal termination the step is given up at once, whatever its budget`() {
        val log = Log()
        val result =
            ran(
                journeyOf(
                        playing("ship", IllegalStateException("down"))
                            .retryBudget(3)
                            .noRetryOnAbnormalTermination()
                    )
                    .reporter(log)
                    .create()
            )
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
        assertEquals(5, log.events.size)
    }

    @Test
    fun `a step that cannot be built aborts the journey, naming the step and carrying the error`() {
        val log = Log()
        val error = IllegalArgumentException("no carrier")
        val result =
            ran(journeyOf(building("ship", SyncStepFactory { throw error })).reporter(log).create())
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
        assertEquals(
            StepCouldNotBeBuilt(StepName.of("ship"), ByStep, "no carrier"),
            result.abort?.details,
        )
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort?.reason)
        assertSame(error, result.abort?.error)
    }

    @Test
    fun `a step whose factory reads a request it did not declare cannot be built`() {
        val note = Requests.input("note", TypeToken.of(String::class.java))
        val ship =
            building("ship") { got ->
                check(got[note].isNotEmpty())
                SyncStepRun { Outcome.success() }
            }
        val result = ran(journeyOf(ship).data("note", "fragile").create())
        assertEquals(
            StepCouldNotBeBuilt(
                StepName.of("ship"),
                ByStep,
                "Nothing answers input note: String, which was not declared.",
            ),
            result.abort?.details,
        )
    }

    @Test
    fun `an Error from a step is never caught`() {
        val error = OutOfMemoryError("full")
        val thrown =
            assertThrows<OutOfMemoryError> {
                ran(journeyOf(playing("ship", error)).create())
            }
        assertSame(error, thrown)
    }
}
