package org.itineradev.itinera.report.testing

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.ContributionsDiscarded
import org.itineradev.itinera.event.DecidedBy
import org.itineradev.itinera.event.DecidingHook
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.JourneySucceeded
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.RescueCause
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.event.StepRescued
import org.itineradev.itinera.event.StepRescuing
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSkipped
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.report.testing.Fixtures.SEQUENCE
import org.itineradev.itinera.report.testing.Fixtures.charge
import org.itineradev.itinera.report.testing.Fixtures.chargeRescue
import org.itineradev.itinera.report.testing.Fixtures.declined
import org.itineradev.itinera.report.testing.Fixtures.first
import org.itineradev.itinera.report.testing.Fixtures.fraudCheck
import org.itineradev.itinera.report.testing.Fixtures.journey
import org.itineradev.itinera.report.testing.Fixtures.second
import org.itineradev.itinera.report.testing.Fixtures.timestamp
import org.itineradev.itinera.report.testing.Fixtures.workflow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class PathTest {
    @ParameterizedTest
    @FieldSource("flow")
    fun `each event of the flow writes its line as the specification writes it`(
        event: Event,
        line: String,
    ) {
        assertEquals(line, pathLine(event))
    }

    @ParameterizedTest
    @FieldSource("outsideTheFlow")
    fun `an event outside the flow writes no line`(event: Event) {
        assertNull(pathLine(event))
    }

    companion object {
        private val hook = DecidingHook(fraudCheck, StepHook.ON_STEP_FAILURE)

        @JvmField
        val flow =
            listOf(
                argumentSet(
                    "a success",
                    StepSucceeded(SEQUENCE, timestamp, journey, workflow, charge, first),
                    "charge attempt 1 succeeded",
                ),
                argumentSet(
                    "a failure, by its reason's code",
                    StepFailed(SEQUENCE, timestamp, journey, workflow, charge, first, declined),
                    "charge attempt 1 failed: declined",
                ),
                argumentSet(
                    "a skip with a reason",
                    StepSkipped(SEQUENCE, timestamp, journey, workflow, charge, first, declined),
                    "charge attempt 1 skipped: declined",
                ),
                argumentSet(
                    "a skip without a reason",
                    StepSkipped(SEQUENCE, timestamp, journey, workflow, charge, first, null),
                    "charge attempt 1 skipped",
                ),
                argumentSet(
                    "an abnormal termination, by its error's message",
                    StepAbnormalTermination(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        charge,
                        first,
                        "timeout",
                    ),
                    "charge attempt 1 terminated abnormally: timeout",
                ),
                argumentSet(
                    "a retry",
                    StepRetrying(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        charge,
                        first,
                        RetryCause.FAILURE,
                    ),
                    "charge retrying",
                ),
                argumentSet(
                    "giving up after a failure",
                    givenUp(GiveUpCause.Failure),
                    "charge given up: failure",
                ),
                argumentSet(
                    "giving up after an abnormal termination",
                    givenUp(GiveUpCause.AbnormalTermination),
                    "charge given up: abnormal termination",
                ),
                argumentSet(
                    "giving up with the retries exhausted",
                    givenUp(GiveUpCause.RetriesExhausted),
                    "charge given up: retries exhausted",
                ),
                argumentSet(
                    "giving up on a hook's FailWorkflow",
                    givenUp(GiveUpCause.FailWorkflow(declined, hook)),
                    "charge given up: FailWorkflow from fraud_check",
                ),
                argumentSet(
                    "a rescue starting",
                    StepRescuing(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        charge,
                        first,
                        RescueCause.FAILURE,
                        chargeRescue,
                    ),
                    "charge rescuing with charge_rescue",
                ),
                argumentSet(
                    "a rescue's attempt, under the rescue step's own name",
                    StepSucceeded(SEQUENCE, timestamp, journey, workflow, chargeRescue, first),
                    "charge_rescue attempt 1 succeeded",
                ),
                argumentSet(
                    "a rescue ending",
                    StepRescued(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        charge,
                        first,
                        chargeRescue,
                    ),
                    "charge rescued by charge_rescue",
                ),
                argumentSet(
                    "a success the executor decided",
                    JourneySucceeded(SEQUENCE, timestamp, journey, workflow, DecidedBy.Default),
                    "journey succeeded",
                ),
                argumentSet(
                    "a success a hook decided",
                    JourneySucceeded(SEQUENCE, timestamp, journey, workflow, hook),
                    "journey succeeded, decided by fraud_check",
                ),
                argumentSet(
                    "a failure with a reason",
                    failed(JourneyFailure.RetriesExhaustedByFailure(declined)),
                    "journey failed at charge: declined",
                ),
                argumentSet(
                    "a failure with no reason, by its error's message",
                    failed(JourneyFailure.AbnormalTermination("timeout")),
                    "journey failed at charge: timeout",
                ),
                argumentSet(
                    "an abort concerning a step",
                    JourneyAborted(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        StepCouldNotBeBuilt(charge, ByStep, "no gateway"),
                    ),
                    "journey aborted at charge: step could not be built",
                ),
                argumentSet(
                    "an abort concerning no step",
                    JourneyAborted(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        ReporterFailed(null, "disk full"),
                    ),
                    "journey aborted: reporter failed",
                ),
            )

        @JvmField
        val outsideTheFlow =
            listOf(
                argumentSet(
                    "the journey starting",
                    JourneyStarted(SEQUENCE, timestamp, journey, workflow, setOf("amount")),
                ),
                argumentSet(
                    "an attempt starting",
                    AttemptStarted(SEQUENCE, timestamp, journey, workflow, charge, second),
                ),
                argumentSet(
                    "an emitted event",
                    StepInfo(
                        SEQUENCE,
                        timestamp,
                        journey,
                        workflow,
                        charge,
                        first,
                        "card read",
                        null,
                    ),
                ),
                argumentSet(
                    "contributions discarded",
                    ContributionsDiscarded(SEQUENCE, timestamp, journey, workflow, charge, first),
                ),
            )

        private fun givenUp(cause: GiveUpCause): Event =
            StepGivenUp(SEQUENCE, timestamp, journey, workflow, charge, first, cause)

        private fun failed(failure: JourneyFailure): Event =
            JourneyFailed(SEQUENCE, timestamp, journey, workflow, charge, failure)
    }
}
