package org.itineradev.itinera.engine

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.journey.Failure
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor

/** Runs the attempts of each step of [journey], building each through [parts]. */
internal class Attempts<M : Mode>(private val journey: Journey, private val parts: Parts<M>) {
    /**
     * Runs [step] until an attempt succeeds, or until the step is given up, which this returns as
     * the journey's failure.
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
            val error = terminated(step) ?: return succeeded(step, attempt)
            val message = messageOf(error)
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
            when (val decision = afterAbnormalTermination(step, attempt, message)) {
                Decision.Retry -> attempt = retried(step, attempt)
                is Decision.GiveUp -> return givenUp(step, attempt, decision, error)
            }
        }
    }

    private suspend fun retried(step: StepDescriptor<M>, attempt: Attempt): Attempt {
        journey.emit {
            StepRetrying(
                sequence,
                timestamp,
                journeyId,
                workflow,
                step.name,
                attempt,
                RetryCause.ABNORMAL_TERMINATION,
            )
        }
        return Attempt.of(attempt.number + 1)
    }

    private suspend fun givenUp(
        step: StepDescriptor<M>,
        attempt: Attempt,
        decision: Decision.GiveUp,
        error: Throwable,
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

    private suspend fun succeeded(step: StepDescriptor<M>, attempt: Attempt): Failure? {
        journey.emit { StepSucceeded(sequence, timestamp, journeyId, workflow, step.name, attempt) }
        return null
    }

    /**
     * Builds and runs one attempt, and gives the error that ended it in an abnormal termination, or
     * `null` when it succeeded.
     */
    private suspend fun terminated(step: StepDescriptor<M>): Throwable? {
        val running = built(step)
        val outcome =
            parts
                .calling { running.run() }
                .getOrElse { thrown ->
                    return thrown
                }
        return when (outcome) {
            Outcome.Success -> null
            null -> throw unexpected(step, "the step's run returned null, not an outcome")
        }
    }

    private suspend fun built(step: StepDescriptor<M>): Running =
        parts
            .calling { parts.build(step.factory, Unresolved()) }
            .getOrElse { thrown -> throw notBuilt(step, thrown) }
            ?: throw unexpected(step, "the step's factory returned null, not a step")

    private fun notBuilt(step: StepDescriptor<M>, thrown: Throwable): JourneyAbortSignal =
        journey.abort(Abort(StepCouldNotBeBuilt(step.name, ByStep, messageOf(thrown)), thrown))

    private fun unexpected(step: StepDescriptor<M>, message: String): JourneyAbortSignal =
        journey.abort(Abort(UnexpectedResult(step.name, ByStep, message), null))
}
