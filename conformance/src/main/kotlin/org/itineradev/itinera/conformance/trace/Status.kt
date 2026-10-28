package org.itineradev.itinera.conformance.trace

import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepSkipped
import org.itineradev.itinera.event.StepSucceeded

/** The status a step ends a journey in, as the cases name it. */
internal enum class Status(val written: String) {
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    SKIPPED("skipped"),
    NOT_EXECUTED("not executed"),
    RESCUING("rescuing"),
    ABORTED("aborted");

    companion object {
        private val byName = entries.associateBy(Status::written)

        /** The status a case names. */
        fun named(written: String): Status =
            byName[written] ?: throw ModelException("\"$written\" is not a step status")
    }
}

/**
 * The status of [step], read from the journey's [events]: the first event that settles it, which
 * stays final even when the journey is aborted afterwards; otherwise aborted when `journey_aborted`
 * names it; otherwise not executed when no attempt started; and `null` while it was still being
 * attempted. Rescuing waits for rescue steps to run.
 */
internal fun statusOf(events: List<Event>, step: String): Status? =
    events.firstNotNullOfOrNull { settled(it, step) } ?: unsettled(events, step)

private fun unsettled(events: List<Event>, step: String): Status? =
    when {
        events.any { abortsAt(it, step) } -> Status.ABORTED
        attemptsOf(events, step) == 0 -> Status.NOT_EXECUTED
        else -> null
    }

/** The status [event] settles [step] in, if it is one of its own that settles it. */
private fun settled(event: Event, step: String): Status? =
    when {
        event is StepSucceeded && event.step.toString() == step -> Status.SUCCEEDED
        event is StepSkipped && event.step.toString() == step -> Status.SKIPPED
        event is StepGivenUp && event.step.toString() == step -> Status.FAILED
        else -> null
    }

/** Whether [event] is a `journey_aborted` naming [step]. */
internal fun abortsAt(event: Event, step: String): Boolean =
    event is JourneyAborted && event.abort.step?.toString() == step

/** How many attempts of [step] started. */
internal fun attemptsOf(events: List<Event>, step: String): Int = events.count {
    startsAnAttempt(it, step)
}

private fun startsAnAttempt(event: Event, step: String): Boolean =
    event is AttemptStarted && event.step.toString() == step
