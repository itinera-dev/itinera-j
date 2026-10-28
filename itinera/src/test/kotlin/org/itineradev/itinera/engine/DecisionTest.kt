package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
internal class DecisionTest {
    @ParameterizedTest
    @FieldSource("abnormalTerminations")
    fun `an abnormal termination is retried while it may be, and otherwise gives the step up`(
        step: StepDescriptor<Sync>,
        attempt: Int,
        expected: Decision,
    ) {
        assertEquals(expected, afterAbnormalTermination(step, Attempt.of(attempt), "down"))
    }

    @ParameterizedTest
    @FieldSource("failures")
    fun `a failure is retried while the budget lasts, and otherwise gives the step up`(
        step: StepDescriptor<Sync>,
        attempt: Int,
        expected: Decision,
    ) {
        assertEquals(expected, afterFailure(step, Attempt.of(attempt), declined))
    }

    companion object {
        private val declined = Reason("declined")

        @JvmField
        val failures: List<Arguments> =
            listOf(
                argumentSet(
                    "a budget of 0",
                    succeeding("charge").build(),
                    1,
                    Decision.GiveUp(GiveUpCause.Failure, JourneyFailure.Failure(declined)),
                ),
                argumentSet(
                    "a budget that lasts",
                    succeeding("charge").retryBudget(2).build(),
                    2,
                    Decision.Retry(RetryCause.FAILURE),
                ),
                argumentSet(
                    "a budget spent",
                    succeeding("charge").retryBudget(2).build(),
                    3,
                    Decision.GiveUp(
                        GiveUpCause.RetriesExhausted,
                        JourneyFailure.RetriesExhaustedByFailure(declined),
                    ),
                ),
                argumentSet(
                    "retry on abnormal termination turned off",
                    succeeding("charge").retryBudget(2).noRetryOnAbnormalTermination().build(),
                    1,
                    Decision.Retry(RetryCause.FAILURE),
                ),
            )

        private val givenUp =
            Decision.GiveUp(
                GiveUpCause.AbnormalTermination,
                JourneyFailure.AbnormalTermination("down"),
            )

        @JvmField
        val abnormalTerminations: List<Arguments> =
            listOf(
                argumentSet("a budget of 0", succeeding("ship").build(), 1, givenUp),
                argumentSet(
                    "a budget that lasts",
                    succeeding("ship").retryBudget(2).build(),
                    2,
                    Decision.Retry(RetryCause.ABNORMAL_TERMINATION),
                ),
                argumentSet(
                    "a budget spent",
                    succeeding("ship").retryBudget(2).build(),
                    3,
                    Decision.GiveUp(
                        GiveUpCause.RetriesExhausted,
                        JourneyFailure.RetriesExhaustedByAbnormalTermination("down"),
                    ),
                ),
                argumentSet(
                    "retry on abnormal termination turned off",
                    succeeding("ship").retryBudget(2).noRetryOnAbnormalTermination().build(),
                    1,
                    givenUp,
                ),
            )
    }
}
