package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.WorkflowName

/**
 * One event of a journey's event stream, a class for each kind, which Kotlin matches with `when`
 * and Java with `switch`. Only itinera makes events for a journey.
 *
 * Every event carries its kind, its sequence number, its timestamp, the journey ID and the workflow
 * name; what else it carries depends on its kind. Engine events carry keys, never the values of the
 * data bag. An event a step, hook or service provider emits carries the data it was given, as a
 * value captured when it was emitted, which a reporter must not change: every reporter receives the
 * same copy. Events have no format of their own: each reporter writes them as it chooses.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
public sealed interface Event {
    /** The event's kind, which tells what else it carries. */
    public val kind: EventKind

    /** The event's position in its journey's event stream, counted from 1. */
    public val sequence: Long

    /** When the event was emitted, in UTC, which prints in ISO 8601. */
    public val timestamp: Instant

    /** The journey's ID. */
    public val journeyId: JourneyId

    /** The workflow's name. */
    public val workflow: WorkflowName
}

/**
 * An event recording what the executor decided to do next, once every hook that could change it has
 * returned.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
public sealed interface Decision : Event {
    /** Who decided it: the executor's own rule, or the hook whose returned lifecycle decided it. */
    public val decidedBy: DecidedBy
}
