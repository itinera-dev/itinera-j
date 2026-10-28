package org.itineradev.itinera.engine

import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.workflow.StepDescriptor

/** What follows an attempt that did not succeed. */
internal sealed interface Decision {
    /** The step is attempted again. */
    data object Retry : Decision

    /** The step is given up for [cause], failing the journey with [failure]. */
    data class GiveUp(val cause: GiveUpCause, val failure: JourneyFailure) : Decision
}

/**
 * What follows [attempt] of [step] once it ended in an abnormal termination with [message]: a retry
 * while `retry on abnormal termination` is on and the retry budget lasts, otherwise the step is
 * given up, for `retries exhausted` once a budget greater than 0 is spent.
 */
internal fun afterAbnormalTermination(
    step: StepDescriptor<*>,
    attempt: Attempt,
    message: String,
): Decision =
    when {
        !step.retryOnAbnormalTermination || step.retryBudget == 0 ->
            Decision.GiveUp(
                GiveUpCause.AbnormalTermination,
                JourneyFailure.AbnormalTermination(message),
            )
        attempt.number <= step.retryBudget -> Decision.Retry
        else ->
            Decision.GiveUp(
                GiveUpCause.RetriesExhausted,
                JourneyFailure.RetriesExhaustedByAbnormalTermination(message),
            )
    }
