package org.itineradev.itinera.engine

import org.itineradev.itinera.event.EmitterSource
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.JourneyError
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.event.JourneyWarning
import org.itineradev.itinera.event.ValueSource
import org.itineradev.itinera.step.StepName

/**
 * What the reporter of one call of a hook or a service provider in [journey], during [step] if any,
 * emits through, `journey_info`, `journey_warning` and `journey_error` naming [source], and sets
 * global metadata through for [giver], until the call returns and closes it; [closed] says why it
 * reports nothing then.
 */
internal class JourneyEmits(
    journey: Journey,
    step: StepName?,
    giver: ValueSource,
    private val source: EmitterSource,
    override val closed: String,
) : Emitting(journey, step, giver, source) {
    override fun eventOf(stamp: Stamp, severity: Severity, message: String, data: Any?): Event =
        with(stamp) {
            when (severity) {
                Severity.INFO ->
                    JourneyInfo(sequence, timestamp, journeyId, workflow, source, message, data)
                Severity.WARNING ->
                    JourneyWarning(sequence, timestamp, journeyId, workflow, source, message, data)
                Severity.ERROR ->
                    JourneyError(sequence, timestamp, journeyId, workflow, source, message, data)
            }
        }

    override fun kindOf(severity: Severity): EventKind =
        when (severity) {
            Severity.INFO -> EventKind.JOURNEY_INFO
            Severity.WARNING -> EventKind.JOURNEY_WARNING
            Severity.ERROR -> EventKind.JOURNEY_ERROR
        }
}
