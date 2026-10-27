package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Fixtures.SEQUENCE
import org.itineradev.itinera.event.Fixtures.charge
import org.itineradev.itinera.event.Fixtures.declined
import org.itineradev.itinera.event.Fixtures.first
import org.itineradev.itinera.event.Fixtures.journey
import org.itineradev.itinera.event.Fixtures.retryHook
import org.itineradev.itinera.event.Fixtures.timestamp
import org.itineradev.itinera.event.Fixtures.workflow
import org.itineradev.itinera.event.samples.failures
import org.itineradev.itinera.event.samples.givenUp
import org.itineradev.itinera.event.samples.rescued
import org.itineradev.itinera.journey.FailureCause
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class DecisionTest {
    @ParameterizedTest
    @FieldSource("givingUp")
    fun `a step given up names its cause and who decided it`(
        cause: GiveUpCause,
        named: FailureCause,
        decidedBy: DecidedBy,
    ) {
        val givenUp = StepGivenUp(SEQUENCE, timestamp, journey, workflow, charge, first, cause)
        assertEquals(named, givenUp.cause.cause)
        assertEquals(decidedBy, givenUp.decidedBy)
    }

    @ParameterizedTest
    @FieldSource("failing")
    fun `a failed journey names its cause and who decided it`(
        failure: JourneyFailure,
        named: FailureCause,
        decidedBy: DecidedBy,
    ) {
        val failed = JourneyFailed(SEQUENCE, timestamp, journey, workflow, charge, failure)
        assertEquals(named, failed.failure.cause)
        assertEquals(decidedBy, failed.decidedBy)
    }

    @Test
    fun `the samples tell how a step and a journey failed`() {
        assertEquals("FailWorkflow: fraud, decided by audit: on step retry", failures(failedByHook))
        assertEquals("given up after retries exhausted", givenUp(GiveUpCause.RetriesExhausted))
        assertEquals(
            "rescued after abnormal termination",
            rescued(RescueCause.ABNORMAL_TERMINATION),
        )
    }

    companion object {
        private val failedByHook =
            JourneyFailure.FailWorkflow(Fixtures.declined.copy(code = "fraud"), retryHook)

        @JvmField
        val givingUp =
            listOf(
                argumentSet(
                    "after a failure",
                    GiveUpCause.Failure,
                    FailureCause.FAILURE,
                    DecidedBy.Default,
                ),
                argumentSet(
                    "after an abnormal termination",
                    GiveUpCause.AbnormalTermination,
                    FailureCause.ABNORMAL_TERMINATION,
                    DecidedBy.Default,
                ),
                argumentSet(
                    "with retries exhausted",
                    GiveUpCause.RetriesExhausted,
                    FailureCause.RETRIES_EXHAUSTED,
                    DecidedBy.Default,
                ),
                argumentSet(
                    "by a hook",
                    GiveUpCause.FailWorkflow(declined, retryHook),
                    FailureCause.FAIL_WORKFLOW,
                    retryHook,
                ),
            )

        @JvmField
        val failing =
            listOf(
                argumentSet(
                    "after a failure",
                    JourneyFailure.Failure(declined),
                    FailureCause.FAILURE,
                    DecidedBy.Default,
                ),
                argumentSet(
                    "after an abnormal termination",
                    JourneyFailure.AbnormalTermination("timeout"),
                    FailureCause.ABNORMAL_TERMINATION,
                    DecidedBy.Default,
                ),
                argumentSet(
                    "with retries exhausted by a failure",
                    JourneyFailure.RetriesExhaustedByFailure(declined),
                    FailureCause.RETRIES_EXHAUSTED,
                    DecidedBy.Default,
                ),
                argumentSet(
                    "with retries exhausted by an abnormal termination",
                    JourneyFailure.RetriesExhaustedByAbnormalTermination("timeout"),
                    FailureCause.RETRIES_EXHAUSTED,
                    DecidedBy.Default,
                ),
                argumentSet("by a hook", failedByHook, FailureCause.FAIL_WORKFLOW, retryHook),
            )
    }
}
