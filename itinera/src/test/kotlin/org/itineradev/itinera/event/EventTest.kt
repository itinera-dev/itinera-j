package org.itineradev.itinera.event

import org.itineradev.itinera.event.Fixtures.SEQUENCE
import org.itineradev.itinera.event.Fixtures.charge
import org.itineradev.itinera.event.Fixtures.closing
import org.itineradev.itinera.event.Fixtures.declined
import org.itineradev.itinera.event.Fixtures.first
import org.itineradev.itinera.event.Fixtures.journey
import org.itineradev.itinera.event.Fixtures.payments
import org.itineradev.itinera.event.Fixtures.pricing
import org.itineradev.itinera.event.Fixtures.stepHookSource
import org.itineradev.itinera.event.Fixtures.stepSource
import org.itineradev.itinera.event.Fixtures.timestamp
import org.itineradev.itinera.event.Fixtures.workflow
import org.itineradev.itinera.event.samples.describe
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.RetryCause
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class EventTest {
    @ParameterizedTest
    @FieldSource("events")
    fun `every event tells its kind`(event: Event, kind: EventKind) {
        assertEquals(kind, event.kind)
    }

    @Test
    fun `the sample describes an event and who decided it`() {
        val retrying =
            StepRetrying(SEQUENCE, timestamp, journey, workflow, charge, first, RetryCause.FAILURE)
        assertEquals(
            "7 2026-10-27T18:00:00Z order-42 step_retrying, decided by default",
            describe(retrying),
        )
    }

    companion object {
        @JvmField
        val events =
            listOf(
                    EventKind.JOURNEY_STARTED to
                        JourneyStarted(SEQUENCE, timestamp, journey, workflow, setOf("amount")),
                    EventKind.ATTEMPT_STARTED to
                        AttemptStarted(SEQUENCE, timestamp, journey, workflow, charge, first),
                    EventKind.INPUT_ADAPTER_SUPPLIED to
                        InputAdapterSupplied(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "price",
                            pricing,
                        ),
                    EventKind.INPUT_ADAPTER_FAILED to
                        InputAdapterFailed(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "price",
                            pricing,
                        ),
                    EventKind.OPTIONAL_INPUT_ABSENT to
                        OptionalInputAbsent(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            "coupon",
                            stepSource,
                        ),
                    EventKind.SERVICE_PROVIDED to
                        ServiceProvided(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "gateway",
                            payments,
                            "Terminal",
                            null,
                        ),
                    EventKind.SERVICE_PROVIDER_FAILED to
                        ServiceProviderFailed(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "gateway",
                            payments,
                        ),
                    EventKind.GLOBAL_METADATA_SET to
                        GlobalMetadataSet(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            "tenant",
                            stepSource,
                        ),
                    EventKind.STEP_SUCCEEDED to
                        StepSucceeded(SEQUENCE, timestamp, journey, workflow, charge, first),
                    EventKind.STEP_FAILED to
                        StepFailed(SEQUENCE, timestamp, journey, workflow, charge, first, declined),
                    EventKind.STEP_SKIPPED to
                        StepSkipped(SEQUENCE, timestamp, journey, workflow, charge, first, null),
                    EventKind.STEP_ABNORMAL_TERMINATION to
                        StepAbnormalTermination(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "timeout",
                        ),
                    EventKind.HOOK_CALLED to
                        StepHookCalled(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            stepHookSource,
                            Lifecycle.FinishWorkflow,
                        ),
                    EventKind.HOOK_CALLED to
                        WorkflowHookCalled(SEQUENCE, timestamp, journey, workflow, closing),
                    EventKind.CONTRIBUTION_COMMITTED to
                        ContributionCommitted(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            "paid",
                            stepSource,
                        ),
                    EventKind.CONTRIBUTIONS_DISCARDED to
                        ContributionsDiscarded(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                        ),
                    EventKind.DATA_OVERWRITTEN to
                        DataOverwritten(SEQUENCE, timestamp, journey, workflow, "paid", stepSource),
                    EventKind.JOURNEY_ABORTED to
                        JourneyAborted(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            RescueSkipped(charge),
                        ),
                    EventKind.STEP_RETRYING to
                        StepRetrying(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            RetryCause.FAILURE,
                        ),
                    EventKind.STEP_RESCUING to
                        StepRescuing(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            RescueCause.FAILURE,
                            charge,
                        ),
                    EventKind.STEP_RESCUED to
                        StepRescued(SEQUENCE, timestamp, journey, workflow, charge, first, charge),
                    EventKind.STEP_GIVEN_UP to
                        StepGivenUp(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            GiveUpCause.Failure,
                        ),
                    EventKind.JOURNEY_SUCCEEDED to
                        JourneySucceeded(SEQUENCE, timestamp, journey, workflow, DecidedBy.Default),
                    EventKind.JOURNEY_FAILED to
                        JourneyFailed(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            JourneyFailure.Failure(declined),
                        ),
                    EventKind.STEP_INFO to
                        StepInfo(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "charging",
                            null,
                        ),
                    EventKind.STEP_WARNING to
                        StepWarning(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "slow",
                            null,
                        ),
                    EventKind.STEP_ERROR to
                        StepError(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            charge,
                            first,
                            "declined",
                            null,
                        ),
                    EventKind.JOURNEY_INFO to
                        JourneyInfo(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            closing,
                            "closing",
                            null,
                        ),
                    EventKind.JOURNEY_WARNING to
                        JourneyWarning(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            closing,
                            "late",
                            null,
                        ),
                    EventKind.JOURNEY_ERROR to
                        JourneyError(
                            SEQUENCE,
                            timestamp,
                            journey,
                            workflow,
                            closing,
                            "unpaid",
                            null,
                        ),
                )
                .map(::named)

        private fun named(case: Pair<EventKind, Event>) =
            argumentSet(case.second.javaClass.simpleName, case.second, case.first)
    }
}
