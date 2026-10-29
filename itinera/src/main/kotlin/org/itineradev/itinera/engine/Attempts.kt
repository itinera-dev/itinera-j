package org.itineradev.itinera.engine

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ContributionsDiscarded
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.RescueCause
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSkipped
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.journey.Failure
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * Runs the attempts of each step of [journey], each through [attempting], building the step's
 * policies for each attempt through [parts], calling their hooks through [hooks], and handing the
 * first attempt of a step that has a rescue step to [rescuing] when it fails.
 */
internal class Attempts<M : Mode>(
    private val journey: Journey,
    private val parts: Parts<M>,
    private val attempting: Attempting<M>,
    private val hooks: Hooks<M>,
    private val rescuing: Rescuing<M>,
) {
    /**
     * Runs [step] until an attempt succeeds or skips, or until the step or its rescue step is given
     * up or a hook ends the journey, which this returns.
     *
     * @throws JourneyAbortSignal once the journey is aborted.
     */
    @JvmSynthetic
    suspend fun of(step: StepDescriptor<M>): Early? {
        journey.step = step.name
        var attempt = FIRST
        while (true) {
            journey.emit {
                AttemptStarted(sequence, timestamp, journeyId, workflow, step.name, attempt)
            }
            val attempted = Attempted(step, attempt, journey.stepPolicies(parts, step))
            when (val concluded = attempted.concluded(attempting.ended(step, attempt))) {
                Concluded.Retry -> attempt = Attempt.of(attempt.number + 1)
                is Concluded.Over -> return concluded.early
            }
        }
    }

    /** What follows an attempt. */
    private sealed interface Concluded {
        /** The step is attempted again. */
        data object Retry : Concluded

        /** The step is done, and the journey goes on, or ends [early]. */
        data class Over(val early: Early?) : Concluded
    }

    /** [attempt] of [step], with the step's [policies] built for it. */
    private inner class Attempted(
        private val step: StepDescriptor<M>,
        private val attempt: Attempt,
        private val policies: List<BuiltPolicy<StepHook>>,
    ) {
        /**
         * Emits how the attempt [ended], and acts on it, calling the step hooks in their order: a
         * success commits what the attempt contributed before `on step success`, a skip discards it
         * and calls no hook, and a failure or an abnormal termination is decided by the executor's
         * own rule and the hooks.
         */
        suspend fun concluded(ended: Ended): Concluded =
            when (ended) {
                is Ended.Succeeded -> succeeded(ended)
                is Ended.Skipped -> skipped(ended)
                is Ended.Failed -> failed(ended)
                is Ended.Terminated -> terminated(ended)
            }

        private suspend fun succeeded(ended: Ended.Succeeded): Concluded {
            journey.emit {
                StepSucceeded(sequence, timestamp, journeyId, workflow, step.name, attempt)
            }
            journey.commit(ended.contributions, StepSource(step.name, attempt))
            val left = Left(ended.contributions, null, null)
            val returned = hooked(StepHook.ON_STEP_SUCCESS, left) ?: return Concluded.Over(null)
            return when (val lifecycle = returned.lifecycle) {
                is Lifecycle.FinishWorkflow -> Concluded.Over(Early.Finished(returned.by))
                is Lifecycle.FailWorkflow ->
                    Concluded.Over(
                        failing(JourneyFailure.FailWorkflow(lifecycle.reason, returned.by), null)
                    )
            }
        }

        private suspend fun skipped(ended: Ended.Skipped): Concluded {
            journey.emit {
                StepSkipped(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    step.name,
                    attempt,
                    ended.reason,
                )
            }
            journey.emit {
                ContributionsDiscarded(sequence, timestamp, journeyId, workflow, step.name, attempt)
            }
            return Concluded.Over(null)
        }

        private suspend fun failed(ended: Ended.Failed): Concluded {
            journey.emit {
                StepFailed(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    step.name,
                    attempt,
                    ended.reason,
                )
            }
            val left = Left(ended.contributions, ended.reason, null)
            val decision = afterFailure(step, attempt, ended.reason)
            return decided(decision, RescueCause.FAILURE, left, null)
        }

        /**
         * Emits the abnormal termination and calls `on step abnormal termination`, which gives the
         * step up when it fails the workflow; otherwise the step is rescued, or the executor's own
         * rule and the hooks decide, as after a failure.
         */
        private suspend fun terminated(ended: Ended.Terminated): Concluded {
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
            val left = Left(emptyMap(), null, ended.error)
            val returned = hooked(StepHook.ON_STEP_ABNORMAL_TERMINATION, left)
            val lifecycle = returned?.lifecycle
            if (returned != null && lifecycle is Lifecycle.FailWorkflow) {
                return givenUpByHook(lifecycle, returned)
            }
            val decision = afterAbnormalTermination(step, attempt, message)
            return decided(decision, RescueCause.ABNORMAL_TERMINATION, left, ended.error)
        }

        /**
         * Acts on an attempt that left [left], having failed for [cause], or ended with [error].
         * When the step has a rescue step and this is its first attempt, the attempt is handed to
         * the rescue step, with no hook called: the step is attempted again once the rescue step
         * succeeded, and the journey fails otherwise. Else [decision], the executor's own rule,
         * stands: while it allows a retry, `on step retry` is called before `step_retrying`, and
         * gives the step up when it fails the workflow; otherwise the step is given up, then `on
         * step failure` is called, and decides why the journey fails when it fails the workflow.
         */
        private suspend fun decided(
            decision: Decision,
            cause: RescueCause,
            left: Left,
            error: Throwable?,
        ): Concluded {
            val rescue = step.rescue?.takeIf { attempt.isFirst }
            if (rescue != null) {
                val early = rescuing.rescued(step, rescue, attempt, cause, left)
                return early?.let(Concluded::Over) ?: Concluded.Retry
            }
            return when (decision) {
                is Decision.Retry -> retried(decision, left)
                is Decision.GiveUp -> givenUp(decision, left, error)
            }
        }

        private suspend fun retried(decision: Decision.Retry, left: Left): Concluded {
            val cause = decision.cause
            val returned = hooked(StepHook.ON_STEP_RETRY, left.calledFor(cause))
            val lifecycle = returned?.lifecycle
            if (returned != null && lifecycle is Lifecycle.FailWorkflow) {
                return givenUpByHook(lifecycle, returned)
            }
            journey.emit {
                StepRetrying(sequence, timestamp, journeyId, workflow, step.name, attempt, cause)
            }
            return Concluded.Retry
        }

        private suspend fun givenUp(
            decision: Decision.GiveUp,
            left: Left,
            error: Throwable?,
        ): Concluded {
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
            val calledFor = left.calledFor(decision.cause.cause)
            val returned = hooked(StepHook.ON_STEP_FAILURE, calledFor)
            val lifecycle = returned?.lifecycle
            return Concluded.Over(
                if (returned != null && lifecycle is Lifecycle.FailWorkflow) {
                    failing(JourneyFailure.FailWorkflow(lifecycle.reason, returned.by), null)
                } else {
                    failing(decision.failure, error)
                }
            )
        }

        /** Gives the step up because a hook returned [lifecycle], as [decided] says. */
        private suspend fun givenUpByHook(
            lifecycle: Lifecycle.FailWorkflow,
            decided: Decided,
        ): Concluded {
            val cause = GiveUpCause.FailWorkflow(lifecycle.reason, decided.by)
            journey.emit {
                StepGivenUp(sequence, timestamp, journeyId, workflow, step.name, attempt, cause)
            }
            val failure = JourneyFailure.FailWorkflow(lifecycle.reason, decided.by)
            return Concluded.Over(failing(failure, null))
        }

        private suspend fun hooked(hook: StepHook, left: Left): Decided? =
            hooks.ofStep(policies, hook, step.name, attempt, left)

        private fun failing(failure: JourneyFailure, error: Throwable?): Early =
            Early.Failed(step.name, Failure(failure, error))
    }
}
