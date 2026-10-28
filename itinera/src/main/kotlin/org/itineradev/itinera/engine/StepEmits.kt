package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.StepError
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.StepWarning
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName

/**
 * What the step reporter of one attempt of [step] in [journey] emits through, `step_info`,
 * `step_warning` and `step_error`, and sets global metadata through, until the attempt ends and
 * closes it.
 */
internal class StepEmits(
    private val journey: Journey,
    private val step: StepName,
    private val attempt: Attempt,
) : Emits {
    @Volatile private var open = true

    /**
     * @throws JourneyAbortSignal when [data] is not a value, when a reporter fails on the event,
     *   and once the journey is aborted.
     */
    @JvmSynthetic
    override suspend fun emit(severity: Severity, message: String, data: Any?) {
        ensureOpen()
        when (val capture = Values.capture(data)) {
            is Captured -> journey.emit { eventOf(severity, message, capture.value) }
            is NotAValue -> throw notAValue(severity, capture.error)
        }
    }

    /**
     * @throws JourneyAbortSignal when [key] is reserved, when [value] is not a value, when a
     *   reporter fails on the entry, and once the journey is aborted.
     */
    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        ensureOpen()
        journey.setGlobalMetadata(key, value, ByStep, StepSource(step, attempt))
    }

    override fun close() {
        open = false
    }

    private fun ensureOpen() {
        check(open) { "The attempt of $step has ended, so its step reporter reports nothing." }
        journey.ensureNotAborted()
    }

    private fun Stamp.eventOf(severity: Severity, message: String, data: Any?): Event =
        when (severity) {
            Severity.INFO ->
                StepInfo(sequence, timestamp, journeyId, workflow, step, attempt, message, data)
            Severity.WARNING ->
                StepWarning(sequence, timestamp, journeyId, workflow, step, attempt, message, data)
            Severity.ERROR ->
                StepError(sequence, timestamp, journeyId, workflow, step, attempt, message, data)
        }

    private fun notAValue(severity: Severity, error: Throwable?): JourneyAbortSignal =
        journey.abort(
            Abort(
                NotAValueAbort.InEvent(step, ByStep, kindOf(severity), error?.let(::messageOf)),
                error,
            )
        )
}

private fun kindOf(severity: Severity): EventKind =
    when (severity) {
        Severity.INFO -> EventKind.STEP_INFO
        Severity.WARNING -> EventKind.STEP_WARNING
        Severity.ERROR -> EventKind.STEP_ERROR
    }
