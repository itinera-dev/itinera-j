package org.itineradev.itinera.engine

import java.time.Clock
import java.time.Instant
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.WorkflowName

/**
 * Makes the events of one journey, numbering them from 1 and stamping each with the time [clock]
 * gives when it is made, its journey ID and its workflow's name.
 */
internal class Emitter(
    private val journeyId: JourneyId,
    private val workflow: WorkflowName,
    private val clock: Clock,
) {
    private var sequence = 0L

    fun next(make: Stamp.() -> Event): Event =
        Stamp(++sequence, Instant.now(clock), journeyId, workflow).make()
}

/** The fixed fields of one event. */
internal class Stamp(
    val sequence: Long,
    val timestamp: Instant,
    val journeyId: JourneyId,
    val workflow: WorkflowName,
)
