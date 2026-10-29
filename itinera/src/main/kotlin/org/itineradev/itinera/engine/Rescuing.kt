package org.itineradev.itinera.engine

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.RescueCause
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRescued
import org.itineradev.itinera.event.StepRescuing
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.journey.Failure
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * Hands the failed attempts of the steps of [journey] to their rescue steps, running each rescue
 * step's one attempt through [attempting]. No hook is called for a rescue step.
 */
internal class Rescuing<M : Mode>(
    private val journey: Journey,
    private val attempting: Attempting<M>,
) {
    /**
     * Rescues [step], whose [attempt] failed for [cause] and left [left], with [rescue]: records
     * `step_rescuing`, then runs the rescue step, which the journey then names, as its attempt 1.
     * When it succeeds, its contributions are committed and `step_rescued` is recorded, naming
     * [step] again, and this gives `null`, so [step] is attempted again. When it fails or ends in
     * an abnormal termination, it is given up, and this gives the journey's failure, which names
     * the rescue step.
     *
     * @throws JourneyAbortSignal once the journey is aborted.
     */
    @JvmSynthetic
    suspend fun rescued(
        step: StepDescriptor<M>,
        rescue: RescueStepDescriptor<M>,
        attempt: Attempt,
        cause: RescueCause,
        left: Left,
    ): Early? {
        val name = rescueOf(step.name)
        journey.emit {
            StepRescuing(sequence, timestamp, journeyId, workflow, step.name, attempt, cause, name)
        }
        journey.step = name
        journey.emit { AttemptStarted(sequence, timestamp, journeyId, workflow, name, FIRST) }
        return when (val ended = attempting.rescued(step, rescue, left)) {
            is RescueEnded.Succeeded -> succeeded(step.name, attempt, name, ended)
            is RescueEnded.Failed -> failed(name, ended)
            is RescueEnded.Terminated -> terminated(name, ended)
        }
    }

    private suspend fun succeeded(
        step: StepName,
        attempt: Attempt,
        name: StepName,
        ended: RescueEnded.Succeeded,
    ): Early? {
        journey.emit { StepSucceeded(sequence, timestamp, journeyId, workflow, name, FIRST) }
        journey.commit(ended.contributions, StepSource(name, FIRST))
        journey.step = step
        journey.emit { StepRescued(sequence, timestamp, journeyId, workflow, step, attempt, name) }
        return null
    }

    private suspend fun failed(name: StepName, ended: RescueEnded.Failed): Early {
        journey.emit {
            StepFailed(sequence, timestamp, journeyId, workflow, name, FIRST, ended.reason)
        }
        return givenUp(name, GiveUpCause.Failure, JourneyFailure.Failure(ended.reason), null)
    }

    private suspend fun terminated(name: StepName, ended: RescueEnded.Terminated): Early {
        val message = messageOf(ended.error)
        journey.emit {
            StepAbnormalTermination(sequence, timestamp, journeyId, workflow, name, FIRST, message)
        }
        val failure = JourneyFailure.AbnormalTermination(message)
        return givenUp(name, GiveUpCause.AbnormalTermination, failure, ended.error)
    }

    private suspend fun givenUp(
        name: StepName,
        cause: GiveUpCause,
        failure: JourneyFailure,
        error: Throwable?,
    ): Early {
        journey.emit { StepGivenUp(sequence, timestamp, journeyId, workflow, name, FIRST, cause) }
        return Early.Failed(name, Failure(failure, error))
    }
}
