package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.StepError
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.StepWarning
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName

/**
 * What the step reporter of one attempt of [step] in [journey] emits through, `step_info`,
 * `step_warning` and `step_error`, and sets global metadata through, until the attempt ends and
 * closes it.
 */
internal class StepEmits(
    journey: Journey,
    private val step: StepName,
    private val attempt: Attempt,
) : Emitting(journey, step, ByStep, StepSource(step, attempt)) {
    override val closed: String
        get() = "The attempt of $step has ended, so its step reporter reports nothing."

    override fun eventOf(stamp: Stamp, severity: Severity, message: String, data: Any?): Event =
        with(stamp) {
            when (severity) {
                Severity.INFO ->
                    StepInfo(sequence, timestamp, journeyId, workflow, step, attempt, message, data)
                Severity.WARNING ->
                    StepWarning(
                        sequence,
                        timestamp,
                        journeyId,
                        workflow,
                        step,
                        attempt,
                        message,
                        data,
                    )
                Severity.ERROR ->
                    StepError(
                        sequence,
                        timestamp,
                        journeyId,
                        workflow,
                        step,
                        attempt,
                        message,
                        data,
                    )
            }
        }

    override fun kindOf(severity: Severity): EventKind =
        when (severity) {
            Severity.INFO -> EventKind.STEP_INFO
            Severity.WARNING -> EventKind.STEP_WARNING
            Severity.ERROR -> EventKind.STEP_ERROR
        }
}
