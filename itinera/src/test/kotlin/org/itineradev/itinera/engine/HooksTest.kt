package org.itineradev.itinera.engine

import java.io.IOException
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Hooking.audit
import org.itineradev.itinera.engine.Hooking.ledger
import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.ByStepHook
import org.itineradev.itinera.event.ByWorkflowHook
import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.GlobalMetadataSet
import org.itineradev.itinera.event.HookFailed
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.OptionalInputAbsent
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.StepHookSource
import org.itineradev.itinera.event.WrongData
import org.itineradev.itinera.event.WrongType
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.Rebuilds
import org.itineradev.itinera.value.Ticket
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class HooksTest {
    private val charge = StepName.of("charge")
    private val audit = PolicyName.of("audit")
    private val onFailure = ByStepHook(audit, StepHook.ON_STEP_FAILURE)
    private val text = TypeToken.of(String::class.java)

    @AfterEach
    fun rebuildAgain() {
        Rebuilds.fail = false
    }

    @Test
    fun `a step hook receives what it requests, in the order it declares it`() {
        val draft = Requests.fromStep("draft", text)
        val order = Requests.fromWorkflow("order", text)
        val reason = Requests.reason()
        val journeyId = Requests.journeyId()
        val step = Requests.stepName()
        val attempt = Requests.attempt()
        val cause = Requests.failureCause()
        val received = mutableListOf<Any>()
        val failing =
            contributing(
                    "charge",
                    { contributor ->
                        contributor.contribute("draft", "D-1")
                        Outcome.failure("declined")
                    },
                )
                .policy(
                    audit()
                        .onStepFailure(
                            listOf(draft, order, reason, journeyId, step, attempt, cause),
                            SyncHook { _, got ->
                                received.addAll(listOf(got[draft], got[order], got[reason].code))
                                received.addAll(listOf(got[journeyId], got[step], got[attempt]))
                                received.add(got[cause])
                                OnFailure.keepDefault()
                            },
                        )
                        .build()
                )
        val result = ran(journeyOf(failing).data("order", "42").create())
        assertEquals(
            listOf(
                "D-1",
                "42",
                "declined",
                result.journeyId,
                charge,
                Attempt.of(1),
                FailureCause.FAILURE,
            ),
            received,
        )
    }

    @Test
    fun `an optional request for data the step did not contribute is absent, naming the hook`() {
        val log = Log()
        val draft = Requests.optionalFromStep("draft", text)
        val received = mutableListOf<String?>()
        val failing =
            contributing("charge", { Outcome.failure("declined") })
                .policy(
                    audit()
                        .onStepFailure(
                            listOf(draft),
                            SyncHook { _, got ->
                                received += got[draft]
                                OnFailure.keepDefault()
                            },
                        )
                        .build()
                )
        ran(journeyOf(failing).reporter(log).create())
        assertEquals(listOf(null), received)
        val absent = log.events.filterIsInstance<OptionalInputAbsent>().single()
        assertEquals("draft", absent.key)
        assertEquals(
            StepHookSource(audit, StepHook.ON_STEP_FAILURE, charge, Attempt.of(1)),
            absent.requester,
        )
    }

    @Test
    fun `a required request for data the workflow lacks aborts the journey before the hook runs`() {
        val log = Log()
        val order = Requests.fromWorkflow("order", text)
        val failing =
            contributing("charge", { Outcome.failure("declined") })
                .policy(
                    audit()
                        .onStepFailure(listOf(order), SyncHook { _, _ -> error("never called") })
                        .build()
                )
        val result = ran(journeyOf(failing).reporter(log).create())
        assertEquals(
            RequiredDataMissing(charge, MissingData.Key("order", onFailure)),
            result.abort?.details,
        )
        assertEquals(listOf("step_given_up", "journey_aborted"), log.kinds.takeLast(2))
    }

    @Test
    fun `a required request for a reason the attempt did not report aborts the journey`() {
        val reason = Requests.reason()
        val terminating =
            playing("charge", IllegalStateException("down"))
                .policy(
                    audit()
                        .onStepFailure(listOf(reason), SyncHook { _, _ -> error("never called") })
                        .build()
                )
        val result = ran(journeyOf(terminating).create())
        assertEquals(
            RequiredDataMissing(charge, MissingData.FailureReason(onFailure)),
            result.abort?.details,
        )
    }

    @Test
    fun `the error a hook requests is its own copy of what ended the attempt`() {
        val thrown = IllegalStateException("down")
        val error = Requests.error()
        val received = mutableListOf<Throwable>()
        val terminating =
            playing("charge", thrown)
                .policy(
                    audit()
                        .onStepAbnormalTermination(
                            listOf(error),
                            SyncHook { _, got ->
                                received += got[error]
                                OnFailure.keepDefault()
                            },
                        )
                        .build()
                )
        ran(journeyOf(terminating).create())
        val copy = received.single()
        assertNotSame(thrown, copy)
        assertEquals(thrown.javaClass, copy.javaClass)
        assertEquals("down", copy.message)
    }

    @Test
    fun `a hook's contributions are committed as soon as it returns, naming its policy and hook`() {
        val log = Log()
        val contributor = Requests.contributor()
        val failing =
            contributing("charge", { Outcome.failure("declined") })
                .policy(
                    audit()
                        .onStepFailure(
                            listOf(contributor),
                            SyncHook { _, got ->
                                got[contributor].contribute("audit", "declined")
                                OnFailure.keepDefault()
                            },
                        )
                        .build()
                )
        val result = ran(journeyOf(failing).reporter(log).create())
        assertEquals(
            listOf("hook_called", "contribution_committed", "journey_failed"),
            log.kinds.takeLast(3),
        )
        assertEquals(
            StepHookSource(audit, StepHook.ON_STEP_FAILURE, charge, Attempt.of(1)),
            log.events.filterIsInstance<ContributionCommitted>().single().source,
        )
        assertEquals(setOf("audit"), (result.status as JourneyStatus.Failed).dataBag.keys)
    }

    @Test
    fun `a hook's events and global metadata name its policy and hook, before hook_called`() {
        val log = Log()
        val reporter = Requests.hookReporter()
        val source = StepHookSource(audit, StepHook.ON_STEP_SUCCESS, charge, Attempt.of(1))
        val reporting =
            succeeding("charge")
                .policy(
                    audit()
                        .onStepSuccess(
                            listOf(reporter),
                            SyncHook { _, got -> reported(got[reporter]) },
                        )
                        .build()
                )
        ran(journeyOf(reporting).reporter(log).create())
        assertEquals(
            listOf("journey_info", "global_metadata_set", "hook_called"),
            log.kinds.subList(3, 6),
        )
        assertEquals(source, log.events.filterIsInstance<JourneyInfo>().single().source)
        assertEquals(source, log.events.filterIsInstance<GlobalMetadataSet>().single().source)
        assertEquals(listOf("tenant" to "north"), log.metadata.drop(1))
    }

    @Test
    fun `a hook that throws aborts the journey with hook failed, carrying the error`() {
        val thrown = IllegalStateException("ledger down")
        val log = Log()
        val failing =
            succeeding("charge")
                .policy(audit().onStepSuccess(SyncHook { _, _ -> throw thrown }).build())
        val result = ran(journeyOf(failing).reporter(log).create())
        assertEquals(HookFailed(charge, "ledger down"), result.abort?.details)
        assertSame(thrown, result.abort?.error)
        assertEquals(listOf("step_succeeded", "journey_aborted"), log.kinds.takeLast(2))
    }

    @Test
    fun `a reporter failing on a hook's event aborts the journey with reporter failed, not hook failed`() {
        val reporter = Requests.hookReporter()
        val reporting =
            succeeding("charge")
                .policy(
                    audit()
                        .onStepSuccess(
                            listOf(reporter),
                            SyncHook { _, got ->
                                runCatching { got[reporter].info("audited") }
                                OnSuccess.keepDefault()
                            },
                        )
                        .build()
                )
        val result = ran(journeyOf(reporting).reporter(FailingOnInfo).create())
        assertEquals(ReporterFailed(charge, "info refused"), result.abort?.details)
    }

    @Test
    fun `a hook's contributor is closed once it returns`() {
        val kept = mutableListOf<Contributor>()
        val contributor = Requests.contributor()
        val ledger =
            ledger()
                .onWorkflowSuccess(
                    listOf(contributor),
                    SyncWorkflowHook { _, got -> kept += got[contributor] },
                )
                .onWorkflowFailure(SyncWorkflowHook { _, _ -> })
                .build()
        ran(instanceOf(shop().step(succeeding("pack").build()).policy(ledger).build()).create())
        val closed = runCatching { kept.single().contribute("late", "L-1") }.exceptionOrNull()
        assertEquals(
            "The hook on workflow success of policy ledger has ended, so its contributor " +
                "contributes nothing.",
            closed?.message,
        )
    }

    @Test
    fun `a hook reaches the workflow through the roles it requests, and fails with what a role throws`() {
        val thrown = IOException("no channel")
        val told = mutableListOf<String>()
        val result =
            ranAtTheCorner(told, thrown) { messages ->
                messages.tell("paid")
                messages.tell("")
            }
        assertEquals(listOf("paid"), told)
        assertEquals(HookFailed(null, "no channel"), result.abort?.details)
        assertSame(thrown, result.abort?.error)
    }

    @Test
    fun `a role operation that fails aborts the journey, even when the hook catches what it threw`() {
        val thrown = IllegalStateException("no channel")
        val result =
            ranAtTheCorner(mutableListOf(), thrown) { messages ->
                runCatching { messages.tell("") }
            }
        assertEquals(HookFailed(null, "no channel"), result.abort?.details)
        assertSame(thrown, result.abort?.error)
    }

    @Test
    fun `a failure's reason whose details cannot be copied for a hook aborts the journey with wrong type`() {
        val reason = Requests.reason()
        val failing =
            contributing("charge", { Outcome.failure(Reason("declined", null, Ticket("A1"))) })
                .policy(
                    audit()
                        .onStepFailure(listOf(reason), SyncHook { _, _ -> OnFailure.keepDefault() })
                        .build()
                )
        val result = ran(journeyOf(failing).reporter(BreakingTicketsOnceGivenUp).create())
        assertEquals(
            WrongType(charge, WrongData.FailureReason(onFailure), "no more tickets"),
            result.abort?.details,
        )
    }

    @Test
    fun `a workflow hook's requests name it`() {
        val log = Log()
        val order = Requests.optionalFromWorkflow("order", text)
        val ledger = ledger().onWorkflowSuccess(listOf(order), SyncWorkflowHook { _, _ -> }).build()
        ran(
            instanceOf(shop().step(succeeding("pack").build()).policy(ledger).build())
                .reporter(log)
                .create()
        )
        assertEquals(
            ByWorkflowHook(PolicyName.of("ledger"), WorkflowHook.ON_WORKFLOW_SUCCESS),
            log.events.filterIsInstance<OptionalInputAbsent>().single().requester,
        )
    }

    /**
     * Runs a journey of [Corner], telling through [told] and refusing an empty message with
     * [thrown], whose `ledger` does what [onSuccess] does with the role [Messages] once the journey
     * succeeds.
     */
    private fun ranAtTheCorner(
        told: MutableList<String>,
        thrown: Throwable,
        onSuccess: (Messages) -> Unit,
    ): JourneyResult {
        val messages = Requests.role(Messages::class.java)
        val ledger =
            ledger()
                .onWorkflowSuccess(
                    listOf(messages),
                    SyncWorkflowHook { _, got -> onSuccess(got[messages]) },
                )
                .build()
        val descriptor =
            WorkflowDescriptor.builder(Corner::class.java, "shop")
                .step(succeeding("pack").build())
                .role(Messages::class.java)
                .policy(ledger)
                .build()
        return ran(descriptor.instance(Corner(told, thrown)).create())
    }

    private fun reported(reporter: HookReporter): OnSuccess {
        reporter.info("audited")
        reporter.setGlobalMetadata("tenant", "north")
        return OnSuccess.keepDefault()
    }

    /** A role of the shop: telling a customer something. */
    @Role
    fun interface Messages {
        fun tell(message: String)
    }

    /** A shop that tells through its own channel, which refuses an empty message with [thrown]. */
    class Corner(private val told: MutableList<String>, private val thrown: Throwable) : Messages {
        override fun tell(message: String) {
            if (message.isEmpty()) throw thrown
            told += message
        }
    }

    /** Makes a [Ticket] fail to copy once a step is given up, after its reason was captured. */
    private object BreakingTicketsOnceGivenUp : Reporter {
        override fun report(event: Event) {
            if (event.kind == EventKind.STEP_GIVEN_UP) Rebuilds.fail = true
        }

        override fun setGlobalMetadata(key: String, value: Any?) = Unit
    }

    private object FailingOnInfo : Reporter {
        override fun report(event: Event) {
            check(event.kind != EventKind.JOURNEY_INFO) { "info refused" }
        }

        override fun setGlobalMetadata(key: String, value: Any?) = Unit
    }
}
