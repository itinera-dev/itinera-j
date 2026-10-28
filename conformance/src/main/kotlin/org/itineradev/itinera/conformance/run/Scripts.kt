package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Ending as Written
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.step.testing.AsyncStepMock
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder

/** The message of an abnormal termination the case scripts with none. */
private const val UNWRITTEN_MESSAGE = "the scripted step terminated abnormally"

/**
 * The synchronous step mock playing the step's script.
 *
 * @throws NotRunYet when the script asks for what the runner cannot run yet.
 */
internal fun mockOf(script: StepScript): StepMock {
    refuseWhatDoesNotRunYet(script)
    return script.buildFailure?.let(StepMock::failingToBuild) ?: playing(endingsOf(script))
}

/**
 * The asynchronous step mock playing the step's script.
 *
 * @throws NotRunYet when the script asks for what the runner cannot run yet.
 */
internal fun asyncMockOf(script: StepScript): AsyncStepMock {
    refuseWhatDoesNotRunYet(script)
    return script.buildFailure?.let(AsyncStepMock::failingToBuild)
        ?: asyncPlaying(endingsOf(script))
}

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

private fun refuseWhatDoesNotRunYet(script: StepScript) {
    val what = notRunYetIn(script)
    if (what != null) throw NotRunYet(what)
}

private fun notRunYetIn(script: StepScript): String? =
    when {
        script.inputs.isNotEmpty() -> "inputs"
        script.contributions.isNotEmpty() -> "contributions"
        script.hasRescueStep || script.isRescueStep -> "rescue steps"
        else -> null
    }

private fun endingOf(attempt: ScriptedAttempt): Ending {
    if (attempt.contributes.isNotEmpty()) throw NotRunYet("contributions")
    return when (attempt.ending) {
        Written.SUCCESS -> Ending.success()
        Written.ERROR -> Ending.abnormalTermination(attempt.message ?: UNWRITTEN_MESSAGE)
        Written.FAILURE,
        Written.SKIPPED -> throw NotRunYet("an attempt that fails or is skipped")
    }
}
