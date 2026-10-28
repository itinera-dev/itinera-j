package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Ending as Written
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.conformance.model.WorkflowModel
import org.itineradev.itinera.step.testing.AsyncStepMock
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder

/** The message of an abnormal termination the case scripts with none. */
private const val UNWRITTEN_MESSAGE = "the scripted step terminated abnormally"

/** The synchronous step mock playing the step's script, one the runner can run. */
internal fun mockOf(script: StepScript): StepMock =
    script.buildFailure?.let(StepMock::failingToBuild) ?: playing(endingsOf(script))

/** The asynchronous step mock playing the step's script, one the runner can run. */
internal fun asyncMockOf(script: StepScript): AsyncStepMock =
    script.buildFailure?.let(AsyncStepMock::failingToBuild) ?: asyncPlaying(endingsOf(script))

/**
 * What [model] scripts that the runner cannot run yet, or `null` when it can run all of it. A
 * workflow that holds any of it is still built, so its admission and its listing are checked, with
 * each such part a call that throws [NotRunYet], which no journey reaches.
 */
internal fun notRunYetIn(model: WorkflowModel): String? =
    when {
        model.adapters.isNotEmpty() -> "input adapters"
        model.providers.isNotEmpty() -> "service providers"
        else -> model.scripts.values.firstNotNullOfOrNull(::notRunYetIn)
    }

/** What [script] asks for that the runner cannot run yet, or `null` when it can run it. */
internal fun notRunYetIn(script: StepScript): String? =
    when {
        script.inputs.isNotEmpty() -> "inputs"
        script.services.isNotEmpty() -> "services"
        script.contributions.isNotEmpty() -> "contributions"
        script.hasRescueStep || script.isRescueStep -> "rescue steps"
        script.attempts.orEmpty().any(::contributes) -> "contributions"
        script.attempts.orEmpty().any(::failsOrSkips) -> "an attempt that fails or is skipped"
        else -> null
    }

private fun contributes(attempt: ScriptedAttempt): Boolean = attempt.contributes.isNotEmpty()

private fun failsOrSkips(attempt: ScriptedAttempt): Boolean =
    attempt.ending == Written.FAILURE || attempt.ending == Written.SKIPPED

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
        Written.ERROR -> Ending.abnormalTermination(attempt.message ?: UNWRITTEN_MESSAGE)
        Written.FAILURE,
        Written.SKIPPED -> throw NotRunYet("an attempt that fails or is skipped")
    }
