package org.itineradev.itinera.engine

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.JourneyError
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.event.JourneyWarning

/**
 * What the hook reporter of one call of a hook in [journey] emits through, `journey_info`,
 * `journey_warning` and `journey_error`, and sets global metadata through, until the hook returns
 * and closes it.
 */
internal class HookEmits(journey: Journey, private val hooked: Hooked<*>) :
    Emitting(journey, hooked.step, hooked.giver, hooked.source) {
    override val closed: String
        get() = "The $hooked has returned, so its hook reporter reports nothing."

    override fun eventOf(stamp: Stamp, severity: Severity, message: String, data: Any?): Event {
        val source = hooked.source
        return with(stamp) {
            when (severity) {
                Severity.INFO ->
                    JourneyInfo(sequence, timestamp, journeyId, workflow, source, message, data)
                Severity.WARNING ->
                    JourneyWarning(sequence, timestamp, journeyId, workflow, source, message, data)
                Severity.ERROR ->
                    JourneyError(sequence, timestamp, journeyId, workflow, source, message, data)
            }
        }
    }

    override fun kindOf(severity: Severity): EventKind =
        when (severity) {
            Severity.INFO -> EventKind.JOURNEY_INFO
            Severity.WARNING -> EventKind.JOURNEY_WARNING
            Severity.ERROR -> EventKind.JOURNEY_ERROR
        }
}
