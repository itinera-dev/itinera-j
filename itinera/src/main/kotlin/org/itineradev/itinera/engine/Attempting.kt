package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * Builds and runs one attempt of a step of [journey], through [parts], from what [building]
 * resolves, with the handles made for the attempt, which close when it ends.
 */
internal class Attempting<M : Mode>(
    private val journey: Journey,
    private val parts: Parts<M>,
    private val building: Building<M>,
) {
    /**
     * How [attempt] of [step] ended.
     *
     * @throws JourneyAbortSignal once the journey is aborted, even when the step caught the signal
     *   it was given.
     */
    @JvmSynthetic
    suspend fun ended(step: StepDescriptor<M>, attempt: Attempt): Ended {
        val contributions = Contributions(journey, step.name)
        val ran =
            try {
                ran(step, built(step, attempt, contributions))
            } finally {
                contributions.close()
            }
        return ran.fold(
            { outcome -> endedWith(step, outcome, contributions.contributed) },
            Ended::Terminated,
        )
    }

    private suspend fun built(
        step: StepDescriptor<M>,
        attempt: Attempt,
        contributions: Contributions,
    ): Running {
        val got = building.got(step, attempt, mapOf(Handle.CONTRIBUTOR to contributions))
        val running =
            parts
                .calling { parts.build(step.factory, got) }
                .getOrElse { thrown -> throw notBuilt(step, thrown) }
        journey.ensureNotAborted()
        return running ?: throw unexpected(step, "the step's factory returned null, not a step")
    }

    /** What the step's run returned, or what it threw. */
    private suspend fun ran(step: StepDescriptor<M>, running: Running): Result<Outcome> {
        val result = parts.calling { running.run() }
        journey.ensureNotAborted()
        return result.map { outcome ->
            outcome ?: throw unexpected(step, "the step's run returned null, not an outcome")
        }
    }

    private fun endedWith(
        step: StepDescriptor<M>,
        outcome: Outcome,
        kept: Map<String, Captured>,
    ): Ended =
        when (outcome) {
            Outcome.Success -> Ended.Succeeded(kept)
            is Outcome.Failure -> Ended.Failed(checked(step, outcome.reason), kept)
            is Outcome.Skipped -> Ended.Skipped(outcome.reason?.let { checked(step, it) }, kept)
        }

    /**
     * [reason] with a copy of its details that only itinera holds, since the step can still reach
     * the copy its outcome holds.
     *
     * @throws JourneyAbortSignal when they are not a value.
     */
    private fun checked(step: StepDescriptor<M>, reason: Reason): Reason =
        when (val capture = Values.capture(reason.details)) {
            is Captured -> reason.copy(details = capture.value)
            is NotAValue -> throw notAValue(step, capture.error)
        }

    private fun notBuilt(step: StepDescriptor<M>, thrown: Throwable): JourneyAbortSignal =
        journey.abort(Abort(StepCouldNotBeBuilt(step.name, ByStep, messageOf(thrown)), thrown))

    private fun unexpected(step: StepDescriptor<M>, message: String): JourneyAbortSignal =
        journey.abort(Abort(UnexpectedResult(step.name, ByStep, message), null))

    private fun notAValue(step: StepDescriptor<M>, error: Throwable?): JourneyAbortSignal =
        journey.abort(
            Abort(NotAValueAbort.InDetails(step.name, ByStep, error?.let(::messageOf)), error)
        )
}
