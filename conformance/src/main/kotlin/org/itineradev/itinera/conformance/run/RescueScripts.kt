package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Ending as Written
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.step.testing.AsyncRescueStepMock
import org.itineradev.itinera.step.testing.RescueEnding
import org.itineradev.itinera.step.testing.RescueStepMock

/** The synchronous rescue step mock playing the rescue step's script. */
internal fun rescueMockOf(script: StepScript): RescueStepMock =
    script.buildFailure?.let(RescueStepMock::failingToBuild)
        ?: rescueEndingsOf(script).let(::rescuePlaying)

/** The asynchronous rescue step mock playing the rescue step's script. */
internal fun asyncRescueMockOf(script: StepScript): AsyncRescueStepMock =
    script.buildFailure?.let(AsyncRescueStepMock::failingToBuild)
        ?: rescueEndingsOf(script).let(::asyncRescuePlaying)

private fun rescuePlaying(endings: List<RescueEnding>): RescueStepMock =
    RescueStepMock.playing(endings.first(), *endings.drop(1).toTypedArray())

private fun asyncRescuePlaying(endings: List<RescueEnding>): AsyncRescueStepMock =
    AsyncRescueStepMock.playing(endings.first(), *endings.drop(1).toTypedArray())

/** How the rescue step's attempt ends, a success when the case does not say. */
private fun rescueEndingsOf(script: StepScript): List<RescueEnding> =
    script.attempts?.map(::rescueEndingOf) ?: listOf(RescueEnding.success())

private fun rescueEndingOf(attempt: ScriptedAttempt): RescueEnding =
    when (attempt.ending) {
        Written.SUCCESS -> RescueEnding.success()
        Written.FAILURE -> RescueEnding.failure(failureReasonOf(attempt))
        Written.SKIPPED ->
            throw ModelException("Kotlin's types refuse a rescue step that skips: it never does")
        Written.ERROR -> RescueEnding.abnormalTermination(attempt.message ?: UNWRITTEN_MESSAGE)
    }
