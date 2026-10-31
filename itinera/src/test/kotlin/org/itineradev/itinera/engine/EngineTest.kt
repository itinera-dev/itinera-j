package org.itineradev.itinera.engine

import java.time.Instant
import org.itineradev.itinera.engine.Hooking.audit
import org.itineradev.itinera.engine.Hooking.ledger
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.packing
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.ByWorkflowHook
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.WorkflowHookCalled
import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun `no step runs after a step is given up`() {
        val log = Log()
        val error = IllegalStateException("carrier down")
        ran(journeyOf(playing("ship", error), succeeding("bill")).reporter(log).create())
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
    }

    @Test
    fun `no step runs after the journey is aborted, whose events stay with journey_aborted last`() {
        val log = Log()
        val ship = building("ship", SyncStepFactory { throw IllegalArgumentException("no van") })
        ran(journeyOf(succeeding("pack"), ship, succeeding("bill")).reporter(log).create())
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_succeeded",
                "attempt_started",
                "journey_aborted",
            ),
            log.kinds,
        )
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

    @Test
    fun `on workflow success runs before journey_succeeded, and its contributions are in the result`() {
        val log = Log()
        val result = ran(ledgered(succeeding("pack")).reporter(log).create())
        assertEquals(
            listOf("hook_called", "contribution_committed", "journey_succeeded"),
            log.kinds.takeLast(3),
        )
        assertEquals(
            ByWorkflowHook(PolicyName.of("ledger"), WorkflowHook.ON_WORKFLOW_SUCCESS),
            log.events.filterIsInstance<WorkflowHookCalled>().single().source,
        )
        assertEquals("succeeded", ledgerOf((result.status as JourneyStatus.Succeeded).dataBag))
    }

    @Test
    fun `on workflow failure runs before journey_failed`() {
        val log = Log()
        val result =
            ran(ledgered(playing("pack", IllegalStateException("down"))).reporter(log).create())
        assertEquals(
            listOf("step_given_up", "hook_called", "contribution_committed", "journey_failed"),
            log.kinds.takeLast(4),
        )
        assertEquals("failed", ledgerOf((result.status as JourneyStatus.Failed).dataBag))
    }

    @Test
    fun `on workflow success runs when a step hook finishes the workflow`() {
        val log = Log()
        val finishing =
            succeeding("pack")
                .policy(
                    audit().onStepSuccess(SyncHook { _, _ -> OnSuccess.finishWorkflow() }).build()
                )
        ran(ledgered(finishing, succeeding("ship")).reporter(log).create())
        assertEquals(
            WorkflowHook.ON_WORKFLOW_SUCCESS,
            log.events.filterIsInstance<WorkflowHookCalled>().single().source.hook,
        )
    }

    @Test
    fun `no workflow hook runs once the journey is aborted`() {
        val log = Log()
        val aborting =
            succeeding("pack")
                .policy(audit().onStepSuccess(SyncHook { _, _ -> error("down") }).build())
        ran(ledgered(aborting).reporter(log).create())
        assertEquals(
            emptyList<WorkflowHookCalled>(),
            log.events.filterIsInstance<WorkflowHookCalled>(),
        )
    }

    private fun withStep(workflow: WorkflowBuilder<Shop, Sync>, step: StepBuilder<Sync>) =
        workflow.step(step.build())

    private fun ledgerOf(dataBag: DataBag): String? =
        dataBag.read("ledger", TypeToken.of(String::class.java)).orNull()

    /** The instance of a workflow of [steps] whose policy `ledger` notes how the journey ended. */
    private fun ledgered(vararg steps: StepBuilder<Sync>): InstanceBuilder {
        val contributor = Requests.contributor()
        val ledger =
            ledger()
                .onWorkflowSuccess(
                    listOf(contributor),
                    SyncWorkflowHook { _, got ->
                        got[contributor].contribute("ledger", "succeeded")
                    },
                )
                .onWorkflowFailure(
                    listOf(contributor),
                    SyncWorkflowHook { _, got -> got[contributor].contribute("ledger", "failed") },
                )
                .build()
        return instanceOf(steps.fold(shop(), ::withStep).policy(ledger).build())
    }
}
