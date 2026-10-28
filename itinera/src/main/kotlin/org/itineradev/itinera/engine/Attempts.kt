package org.itineradev.itinera.engine

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ContributionsDiscarded
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSkipped
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.journey.Failure
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor

/** Runs the attempts of each step of [journey], each through [attempting]. */
internal class Attempts<M : Mode>(
    private val journey: Journey,
    private val attempting: Attempting<M>,
) {
    /**
     * Runs [step] until an attempt succeeds or skips, or until the step is given up, which this
     * returns as the journey's failure.
     *
     * @throws JourneyAbortSignal once the journey is aborted.
     */
    @JvmSynthetic
    suspend fun of(step: StepDescriptor<M>): Failure? {
        journey.step = step.name
        var attempt = Attempt.of(1)
        while (true) {
            journey.emit {
                AttemptStarted(sequence, timestamp, journeyId, workflow, step.name, attempt)
            }
            val ended = attempting.ended(step, attempt)
            when (val decision = decided(step, attempt, ended) ?: return null) {
                is Decision.Retry -> attempt = retried(step, attempt, decision.cause)
                is Decision.GiveUp -> return givenUp(step, attempt, decision, errorOf(ended))
            }
        }
    }

    /**
     * Emits how [attempt] of [step] [ended], and gives what follows a failure or an abnormal
     * termination, or `null` once a success or a skip lets the journey go on.
     */
    private suspend fun decided(
        step: StepDescriptor<M>,
        attempt: Attempt,
        ended: Ended,
    ): Decision? =
        when (ended) {
            is Ended.Succeeded -> succeeded(step, attempt, ended)
            is Ended.Skipped -> skipped(step, attempt, ended)
            is Ended.Failed -> failed(step, attempt, ended)
            is Ended.Terminated -> terminated(step, attempt, ended)
        }

    private suspend fun succeeded(
        step: StepDescriptor<M>,
        attempt: Attempt,
        ended: Ended.Succeeded,
    ): Decision? {
        journey.emit { StepSucceeded(sequence, timestamp, journeyId, workflow, step.name, attempt) }
        journey.commit(ended.contributions, StepSource(step.name, attempt))
        return null
    }

    private suspend fun skipped(
        step: StepDescriptor<M>,
        attempt: Attempt,
        ended: Ended.Skipped,
    ): Decision? {
        journey.emit {
            StepSkipped(sequence, timestamp, journeyId, workflow, step.name, attempt, ended.reason)
        }
        journey.emit {
            ContributionsDiscarded(sequence, timestamp, journeyId, workflow, step.name, attempt)
        }
        return null
    }

    private suspend fun failed(
        step: StepDescriptor<M>,
        attempt: Attempt,
        ended: Ended.Failed,
    ): Decision {
        journey.emit {
            StepFailed(sequence, timestamp, journeyId, workflow, step.name, attempt, ended.reason)
        }
        return afterFailure(step, attempt, ended.reason)
    }

    private suspend fun terminated(
        step: StepDescriptor<M>,
        attempt: Attempt,
        ended: Ended.Terminated,
    ): Decision {
        val message = messageOf(ended.error)
        journey.emit {
            StepAbnormalTermination(
                sequence,
                timestamp,
                journeyId,
                workflow,
                step.name,
                attempt,
                message,
            )
        }
        return afterAbnormalTermination(step, attempt, message)
    }

    private suspend fun retried(
        step: StepDescriptor<M>,
        attempt: Attempt,
        cause: RetryCause,
    ): Attempt {
        journey.emit {
            StepRetrying(sequence, timestamp, journeyId, workflow, step.name, attempt, cause)
        }
        return Attempt.of(attempt.number + 1)
    }

    private suspend fun givenUp(
        step: StepDescriptor<M>,
        attempt: Attempt,
        decision: Decision.GiveUp,
        error: Throwable?,
    ): Failure {
        journey.emit {
            StepGivenUp(
                sequence,
                timestamp,
                journeyId,
                workflow,
                step.name,
                attempt,
                decision.cause,
            )
        }
        return Failure(decision.failure, error)
    }
}

/** The error that ended an attempt in an abnormal termination, or `null` for any other ending. */
private fun errorOf(ended: Ended): Throwable? = (ended as? Ended.Terminated)?.error
