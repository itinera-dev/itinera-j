package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Hooking.audit
import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.engine.Renewals.renewing
import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.DataOverwritten
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.RescueCause
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRescuing
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSkipped
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Sync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AttemptsTest {
    private val charge = StepName.of("charge")
    private val declined = Reason("declined", "The card was declined.", mapOf("bank" to "05"))

    @Test
    fun `a failure with no retry budget gives the step up and fails the journey with its reason`() {
        val log = Log()
        val result =
            ran(
                journeyOf(contributing("charge", { Outcome.failure(declined) }))
                    .reporter(log)
                    .create()
            )
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_failed",
                "step_given_up",
                "journey_failed",
            ),
            log.kinds,
        )
        assertEquals(declined, (log.events[2] as StepFailed).reason)
        assertEquals(FailureCause.FAILURE, result.failure?.cause)
        assertEquals(declined, result.failure?.reason)
        assertNull(result.failure?.error)
    }

    @Test
    fun `a failure is retried while the retry budget lasts`() {
        val log = Log()
        val result =
            ran(
                journeyOf(
                        contributing("charge", { Outcome.failure("down") }, { Outcome.success() })
                            .retryBudget(1)
                    )
                    .reporter(log)
                    .create()
            )
        assertEquals(RetryCause.FAILURE, log.events.filterIsInstance<StepRetrying>().single().cause)
        assertTrue(result.isSucceeded)
    }

    @Test
    fun `a failure past the retry budget gives the step up with retries exhausted and the last reason`() {
        val result =
            ran(
                journeyOf(
                        contributing(
                                "charge",
                                { Outcome.failure("down") },
                                { Outcome.failure(declined) },
                            )
                            .retryBudget(1)
                    )
                    .create()
            )
        assertEquals(FailureCause.RETRIES_EXHAUSTED, result.failure?.cause)
        assertEquals(declined, result.failure?.reason)
    }

    @Test
    fun `a skipped step discards its contributions and the journey goes on`() {
        val log = Log()
        val discounting =
            contributing(
                "discount",
                { contributor ->
                    contributor.contribute("discount", 10L)
                    Outcome.skipped("no discount")
                },
            )
        val result = ran(journeyOf(discounting, succeeding("ship")).reporter(log).create())
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_skipped",
                "contributions_discarded",
                "attempt_started",
                "step_succeeded",
                "journey_succeeded",
            ),
            log.kinds,
        )
        assertEquals(Reason("no discount"), (log.events[2] as StepSkipped).reason)
        assertEquals(emptySet<String>(), dataBagOf(result.status).keys)
    }

    @Test
    fun `a successful attempt commits its contributions after step_succeeded`() {
        val log = Log()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("receipt", "R-1")
                    contributor.contribute("charged", 40L)
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).reporter(log).create())
        val source = StepSource(charge, Attempt.of(1))
        assertEquals(
            listOf("receipt", "charged"),
            log.events.filterIsInstance<ContributionCommitted>().map(ContributionCommitted::key),
        )
        assertEquals(
            setOf(source),
            log.events.filterIsInstance<ContributionCommitted>().map { it.source }.toSet(),
        )
        assertEquals("step_succeeded", log.kinds[2])
        assertEquals(setOf("receipt", "charged"), dataBagOf(result.status).keys)
    }

    @Test
    fun `a contribution over a key already in the data bag is committed and recorded as an overwrite`() {
        val log = Log()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("amount", 2L)
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).data("amount", 1L).reporter(log).create())
        assertEquals(
            listOf("step_succeeded", "contribution_committed", "data_overwritten"),
            log.kinds.subList(2, 5),
        )
        val overwritten = log.events[4] as DataOverwritten
        assertEquals("amount", overwritten.key)
        assertEquals(StepSource(charge, Attempt.of(1)), overwritten.source)
        assertEquals(
            2L,
            dataBagOf(result.status)
                .read("amount", TypeToken.of(Long::class.javaObjectType))
                .orNull(),
        )
    }

    @Test
    fun `only the successful attempt's contributions are committed`() {
        val charging =
            contributing(
                    "charge",
                    { contributor ->
                        contributor.contribute("draft", "D-1")
                        Outcome.failure("unreachable")
                    },
                    { contributor ->
                        contributor.contribute("receipt", "R-1")
                        Outcome.success()
                    },
                )
                .retryBudget(1)
        val result = ran(journeyOf(charging).create())
        assertEquals(setOf("receipt"), dataBagOf(result.status).keys)
    }

    @Test
    fun `an abnormal termination carries no contributions`() {
        val log = Log()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("draft", "D-1")
                    throw IllegalStateException("down")
                },
            )
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
        assertEquals(
            emptyList<ContributionCommitted>(),
            log.events.filterIsInstance<ContributionCommitted>(),
        )
        assertEquals(emptySet<String>(), (result.status as JourneyStatus.Failed).dataBag.keys)
    }

    @Test
    fun `an abnormal termination is rescued, though the step retries abnormal terminations`() {
        val log = Log()
        val charging = playing("charge", IllegalStateException("down"), null).rescue(renewing())
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            RescueCause.ABNORMAL_TERMINATION,
            log.events.filterIsInstance<StepRescuing>().single().cause,
        )
        assertTrue(result.isSucceeded)
    }

    @Test
    fun `on step abnormal termination is called before the step is rescued, and on step retry never`() {
        val log = Log()
        val charging =
            playing("charge", IllegalStateException("down"), null)
                .rescue(renewing())
                .policy(watching(OnFailure.keepDefault()))
        ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            listOf("step_abnormal_termination", "hook_called", "step_rescuing"),
            log.kinds.subList(2, 5),
        )
        assertEquals(listOf("on step abnormal termination"), called)
    }

    @Test
    fun `FailWorkflow from on step abnormal termination gives the step up instead of rescuing it`() {
        val log = Log()
        val charging =
            playing("charge", IllegalStateException("down"))
                .rescue(renewing())
                .policy(watching(OnFailure.failWorkflow("stop")))
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(emptyList<StepRescuing>(), log.events.filterIsInstance<StepRescuing>())
        assertEquals(
            GiveUpCause.FailWorkflow::class,
            log.events.filterIsInstance<StepGivenUp>().single().cause::class,
        )
        assertEquals(Reason("stop"), result.failure?.reason)
    }

    @Test
    fun `a step that fails again after its rescue is given up, calls on step failure, and is not rescued again`() {
        val log = Log()
        val charging =
            contributing("charge", { Outcome.failure("expired") }, { Outcome.failure("declined") })
                .rescue(renewing())
                .policy(watching(OnFailure.keepDefault()))
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(1, log.events.filterIsInstance<StepRescuing>().size)
        assertEquals(
            StepGivenUp(
                10,
                Journeys.clock.instant(),
                result.journeyId,
                log.events[9].workflow,
                charge,
                Attempt.of(2),
                GiveUpCause.Failure,
            ),
            log.events[9],
        )
        assertEquals(listOf("on step failure"), called)
        assertEquals(Reason("declined"), result.failure?.reason)
    }

    private val called = mutableListOf<String>()

    /**
     * A policy whose `on step abnormal termination`, `on step retry` and `on step failure` each
     * record their call, then return [returned].
     */
    private fun watching(returned: OnFailure): StepPolicyDescriptor<*, Sync> =
        audit()
            .onStepAbnormalTermination(recording("on step abnormal termination", returned))
            .onStepRetry(recording("on step retry", returned))
            .onStepFailure(recording("on step failure", returned))
            .build()

    private fun <K> recording(
        hook: String,
        returned: OnFailure,
    ): SyncHook<Hooking.Audit, K, OnFailure> = SyncHook { _, _ ->
        called += hook
        returned
    }

    private fun dataBagOf(status: JourneyStatus) = (status as JourneyStatus.Succeeded).dataBag
}
