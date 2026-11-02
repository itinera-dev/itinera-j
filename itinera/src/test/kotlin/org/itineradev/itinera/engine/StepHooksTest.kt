package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Hooking.audit
import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.DecidingHook
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.JourneySucceeded
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepHookCalled
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class StepHooksTest {
    private val fraud = Reason("fraud")
    private val audit = PolicyName.of("audit")

    @Test
    fun `on step success is called once the attempt's contributions are committed`() {
        val log = Log()
        val charging =
            contributing(
                    "charge",
                    { contributor ->
                        contributor.contribute("receipt", "R-1")
                        Outcome.success()
                    },
                )
                .policy(audit().onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() }).build())
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_succeeded",
                "contribution_committed",
                "hook_called",
                "journey_succeeded",
            ),
            log.kinds,
        )
        assertNull(log.events.filterIsInstance<StepHookCalled>().single().lifecycle)
        assertEquals(true, result.isSucceeded)
    }

    @Test
    fun `a failure with retry budget left calls on step retry, with the cause, before step_retrying`() {
        val log = Log()
        val causes = mutableListOf<RetryCause>()
        val cause = Requests.retryCause()
        val charging =
            contributing("charge", { Outcome.failure("down") }, { Outcome.success() })
                .retryBudget(1)
                .policy(
                    audit()
                        .onStepRetry(
                            listOf(cause),
                            SyncHook { _, got ->
                                causes += got[cause]
                                OnFailure.keepDefault()
                            },
                        )
                        .build()
                )
        ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            listOf("step_failed", "hook_called", "step_retrying", "attempt_started"),
            log.kinds.subList(2, 6),
        )
        assertEquals(listOf(RetryCause.FAILURE), causes)
    }

    @Test
    fun `a step given up calls only on step failure, after step_given_up, with its cause`() {
        val log = Log()
        val called = mutableListOf<Any>()
        val cause = Requests.failureCause()
        val charging =
            contributing("charge", { Outcome.failure("down") }, { Outcome.failure("down") })
                .retryBudget(1)
                .policy(
                    audit()
                        .onStepRetry(
                            SyncHook { _, _ ->
                                called += StepHook.ON_STEP_RETRY
                                OnFailure.keepDefault()
                            }
                        )
                        .onStepFailure(
                            listOf(cause),
                            SyncHook { _, got ->
                                called += got[cause]
                                OnFailure.keepDefault()
                            },
                        )
                        .build()
                )
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(listOf(StepHook.ON_STEP_RETRY, FailureCause.RETRIES_EXHAUSTED), called)
        assertEquals(
            listOf("step_failed", "step_given_up", "hook_called", "journey_failed"),
            log.kinds.takeLast(4),
        )
        assertEquals(FailureCause.RETRIES_EXHAUSTED, result.failure?.cause)
    }

    @Test
    fun `an abnormal termination calls on step abnormal termination before the step decision`() {
        val log = Log()
        val charging =
            playing("charge", IllegalStateException("down"))
                .policy(
                    audit()
                        .onStepAbnormalTermination(SyncHook { _, _ -> OnFailure.keepDefault() })
                        .build()
                )
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            listOf("step_abnormal_termination", "hook_called", "step_given_up", "journey_failed"),
            log.kinds.takeLast(4),
        )
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
    }

    @Test
    fun `a skipped step calls no hook`() {
        val log = Log()
        val skipping =
            contributing("charge", { Outcome.skipped() })
                .policy(audit().onStepSuccess(SyncHook { _, _ -> error("never called") }).build())
        ran(journeyOf(skipping).reporter(log).create())
        assertEquals(emptyList<StepHookCalled>(), log.events.filterIsInstance<StepHookCalled>())
    }

    @Test
    fun `finishWorkflow from on step success ends the journey at once as succeeded, decided by it`() {
        val log = Log()
        val finishing =
            succeeding("charge")
                .policy(
                    audit().onStepSuccess(SyncHook { _, _ -> OnSuccess.finishWorkflow() }).build()
                )
        val result = ran(journeyOf(finishing, succeeding("ship")).reporter(log).create())
        assertEquals(
            DecidingHook(audit, StepHook.ON_STEP_SUCCESS),
            log.events.filterIsInstance<JourneySucceeded>().single().decidedBy,
        )
        assertEquals(1, log.kinds.count { kind -> kind == "attempt_started" })
        assertEquals(
            Lifecycle.FinishWorkflow,
            log.events.filterIsInstance<StepHookCalled>().single().lifecycle,
        )
        assertEquals(true, result.isSucceeded)
    }

    @Test
    fun `failWorkflow from on step success keeps the step succeeded and fails the journey with its reason`() {
        val log = Log()
        val failing =
            succeeding("charge")
                .policy(
                    audit()
                        .onStepSuccess(SyncHook { _, _ -> OnSuccess.failWorkflow(fraud) })
                        .build()
                )
        val result = ran(journeyOf(failing, succeeding("ship")).reporter(log).create())
        assertEquals(
            listOf("step_succeeded", "hook_called", "journey_failed"),
            log.kinds.takeLast(3),
        )
        assertFailedBy(StepHook.ON_STEP_SUCCESS, log)
        assertEquals(FailureCause.FAIL_WORKFLOW, result.failure?.cause)
        assertEquals(fraud, result.failure?.reason)
    }

    @Test
    fun `failWorkflow from on step retry gives the step up for it, with no failure hook`() {
        val log = Log()
        val retrying =
            playing("charge", IllegalStateException("down"), null)
                .retryBudget(1)
                .policy(
                    failingOn()
                        .onStepRetry(SyncHook { _, _ -> OnFailure.failWorkflow(fraud) })
                        .build()
                )
        val result = ran(journeyOf(retrying).reporter(log).create())
        assertEquals(
            GiveUpCause.FailWorkflow(fraud, DecidingHook(audit, StepHook.ON_STEP_RETRY)),
            log.events.filterIsInstance<StepGivenUp>().single().cause,
        )
        assertEquals(
            listOf("hook_called", "step_given_up", "journey_failed"),
            log.kinds.takeLast(3),
        )
        assertEquals(fraud, result.failure?.reason)
    }

    @Test
    fun `failWorkflow from on step abnormal termination gives the step up for it, with no failure hook`() {
        val log = Log()
        val terminating =
            playing("charge", IllegalStateException("down"))
                .policy(
                    failingOn()
                        .onStepAbnormalTermination(
                            SyncHook { _, _ -> OnFailure.failWorkflow(fraud) }
                        )
                        .build()
                )
        val result = ran(journeyOf(terminating).reporter(log).create())
        assertEquals(
            GiveUpCause.FailWorkflow(
                fraud,
                DecidingHook(audit, StepHook.ON_STEP_ABNORMAL_TERMINATION),
            ),
            log.events.filterIsInstance<StepGivenUp>().single().cause,
        )
        assertEquals(FailureCause.FAIL_WORKFLOW, result.failure?.cause)
        assertNull(result.failure?.error)
    }

    @Test
    fun `failWorkflow from on step failure fails the journey with its reason, decided by it`() {
        val log = Log()
        val failing =
            contributing("charge", { Outcome.failure("declined") })
                .policy(
                    audit()
                        .onStepFailure(SyncHook { _, _ -> OnFailure.failWorkflow(fraud) })
                        .build()
                )
        val result = ran(journeyOf(failing).reporter(log).create())
        assertEquals(GiveUpCause.Failure, log.events.filterIsInstance<StepGivenUp>().single().cause)
        assertFailedBy(StepHook.ON_STEP_FAILURE, log)
        assertEquals(fraud, result.failure?.reason)
    }

    /** `audit`, with an `on step failure` that must never be called. */
    private fun failingOn() =
        audit().onStepFailure(SyncHook { _, _ -> error("on step failure is never called") })

    private fun assertFailedBy(hook: StepHook, log: Log) {
        val failed = log.events.filterIsInstance<JourneyFailed>().single()
        assertEquals(StepName.of("charge"), failed.step)
        assertEquals(
            JourneyFailure.FailWorkflow(fraud, DecidingHook(audit, hook)),
            failed.failure,
        )
    }
}
