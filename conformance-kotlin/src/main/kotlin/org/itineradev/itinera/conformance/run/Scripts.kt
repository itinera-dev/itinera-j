package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Ending as Written
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.testing.AsyncStepMock
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder

/** The message of an abnormal termination the case scripts with none. */
internal const val UNWRITTEN_MESSAGE = "the scripted step terminated abnormally"

/** The synchronous step mock playing the step's script, one the runner can run. */
internal fun mockOf(script: StepScript): StepMock =
    script.buildFailure?.let(StepMock::failingToBuild) ?: playing(endingsOf(script))

/** The asynchronous step mock playing the step's script, one the runner can run. */
internal fun asyncMockOf(script: StepScript): AsyncStepMock =
    script.buildFailure?.let(AsyncStepMock::failingToBuild) ?: asyncPlaying(endingsOf(script))

private fun playing(endings: List<Ending>): StepMock =
    StepMock.playing(endings.first(), *endings.drop(1).toTypedArray())

private fun asyncPlaying(endings: List<Ending>): AsyncStepMock =
    AsyncStepMock.playing(endings.first(), *endings.drop(1).toTypedArray())

/** The step's retry budget and retry on abnormal termination, as its script says. */
internal fun <M : Mode> StepBuilder<M>.scriptedBy(script: StepScript): StepBuilder<M> {
    script.retries?.let(::retryBudget)
    if (!script.retriesAbnormalTerminations) noRetryOnAbnormalTermination()
    return this
}

/** How each attempt of the step ends, a success for every attempt when the case does not say. */
private fun endingsOf(script: StepScript): List<Ending> =
    script.attempts?.map(::endingOf) ?: listOf(Ending.success())

private fun endingOf(attempt: ScriptedAttempt): Ending =
    when (attempt.ending) {
        Written.SUCCESS -> Ending.success()
        Written.FAILURE -> Ending.failure(failureReasonOf(attempt))
        Written.SKIPPED -> Ending.Skipped(attempt.code?.let { code -> reasonOf(attempt, code) })
        Written.ERROR -> Ending.abnormalTermination(attempt.message ?: UNWRITTEN_MESSAGE)
    }

internal fun failureReasonOf(attempt: ScriptedAttempt): Reason =
    reasonOf(
        attempt,
        attempt.code ?: throw ModelException("attempt ${attempt.number} fails with no code"),
    )

private fun reasonOf(attempt: ScriptedAttempt, code: String): Reason =
    Reason(code, attempt.message, attempt.details)
