package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.event.Event

/**
 * The event stream of one of the scenario's [journeys]: what the reporter the runner's dispatchers
 * hold received of it, or, under the default dispatcher, what the first reporter the workflow lists
 * that never failed received, made for that journey. With no [journey], it is everything the held
 * reporter received, or nothing under the default dispatcher, since no listed reporter was made.
 */
internal fun streamOf(
    model: Model,
    recorders: Recorders,
    journeys: List<Journey>,
    journey: Journey?,
): List<Event> {
    val dispatching = model.dispatching
    if (dispatching == Dispatching.Default) return listedStream(model, recorders, journeys, journey)
    val held = (dispatching as? Dispatching.Holding)?.reporter ?: RUNNERS_RECORDER
    val events = recorders.made(held).flatMap(ScenarioReporter::events)
    return journey?.let { ofJourney(events, it) } ?: events
}

private fun listedStream(
    model: Model,
    recorders: Recorders,
    journeys: List<Journey>,
    journey: Journey?,
): List<Event> =
    if (journey == null) emptyList()
    else firstThatNeverFailed(model, recorders, journeys.indexOf(journey)).events

private fun ofJourney(events: List<Event>, journey: Journey): List<Event> = events.filter {
    isOf(it, journey.journeyId)
}

/** Whether [event] belongs to the journey whose ID is [journeyId]. */
internal fun isOf(event: Event, journeyId: String?): Boolean =
    event.journeyId.toString() == journeyId

private fun firstThatNeverFailed(model: Model, recorders: Recorders, place: Int): ScenarioReporter =
    model.workflow.reporters
        .orEmpty()
        .mapNotNull { madeAt(recorders, it, place) }
        .firstOrNull(::neverFailed)
        ?: throw ModelException(
            "every reporter the workflow lists failed, so no complete stream is left to read"
        )

private fun madeAt(recorders: Recorders, name: String, place: Int): ScenarioReporter? =
    recorders.made(name).getOrNull(place)

private fun neverFailed(reporter: ScenarioReporter): Boolean = !reporter.failed
