package org.itineradev.itinera.conformance.trace

import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRescuing
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
 * The status of [step], read from the journey's [events] in order: the first event that settles it
 * stays final even when the journey is aborted afterwards; `step_rescuing` makes it rescuing until
 * its next `attempt_started`; `journey_aborted` aborts it when it names it or while it is rescuing.
 * Otherwise it is not executed when no attempt started, and `null` while it was still being
 * attempted.
 */
internal fun statusOf(events: List<Event>, step: String): Status? =
    events.fold(null) { status: Status?, event -> next(status, event, step) }
        ?: Status.NOT_EXECUTED.takeIf { attemptsOf(events, step) == 0 }

/** The status of [step] once [event] follows its [status], which a final status keeps. */
private fun next(status: Status?, event: Event, step: String): Status? =
    if (status == null || status == Status.RESCUING) after(status, event, step) else status

private fun after(status: Status?, event: Event, step: String): Status? =
    when {
        event is JourneyAborted && (status == Status.RESCUING || abortsAt(event, step)) ->
            Status.ABORTED
        status == Status.RESCUING && startsAnAttempt(event, step) -> null
        else -> settled(event, step) ?: status
    }

/** The status [event] puts [step] in, if it is one of its own that does. */
private fun settled(event: Event, step: String): Status? =
    when {
        event is StepSucceeded && event.step.toString() == step -> Status.SUCCEEDED
        event is StepSkipped && event.step.toString() == step -> Status.SKIPPED
        event is StepGivenUp && event.step.toString() == step -> Status.FAILED
        event is StepRescuing && event.step.toString() == step -> Status.RESCUING
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
