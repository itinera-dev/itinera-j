package org.itineradev.itinera.engine

import java.time.Clock
import org.itineradev.itinera.event.DecidedBy
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.JourneySucceeded
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.workflow.Mode

/**
 * Runs journeys of mode [M], calling their parts through [parts] and stamping their events with the
 * time [clock] gives.
 *
 * Part of itinera's engine, not of its API.
 */
public class Engine<M : Mode>
public constructor(private val parts: Parts<M>, private val clock: Clock = Clock.systemUTC()) {
    /**
     * Runs the one journey of [instance] and returns its result. Nothing that happens in the
     * journey escapes, except an [Error] and, in the asynchronous mode, the cancellation of the
     * coroutine running it.
     *
     * @throws Refusal before any journey and any event.
     */
    @JvmSynthetic
    public suspend fun run(instance: WorkflowInstance<M>): JourneyResult {
        if (!instance.claim()) throw Refusal(Refused.InstanceRan)
        val started = Start(parts).of(instance)
        val journey =
            Journey(
                Emitter(instance.journeyId, instance.descriptor.name, clock),
                started.delivery,
                started.failures,
                parts,
                DataBags.of(started.initial.data.values),
            )
        return JourneyResult(instance.journeyId, statusOf(journey, instance))
    }

    private suspend fun statusOf(journey: Journey, instance: WorkflowInstance<M>): JourneyStatus =
        try {
            ran(journey, instance)
        } catch (signal: JourneyAbortSignal) {
            journey.deliverAborted(signal.abort)
            JourneyStatus.Aborted(signal.abort)
        }

    private suspend fun ran(journey: Journey, instance: WorkflowInstance<M>): JourneyStatus {
        val keys = journey.data.keys
        journey.emit { JourneyStarted(sequence, timestamp, journeyId, workflow, keys) }
        val attempts = Attempts(journey, parts, Building(journey, parts, instance))
        for (step in instance.descriptor.steps) {
            val givenUp = attempts.of(step) ?: continue
            journey.emit {
                JourneyFailed(sequence, timestamp, journeyId, workflow, step.name, givenUp.details)
            }
            return JourneyStatus.Failed(givenUp, journey.data)
        }
        journey.step = null
        journey.emit {
            JourneySucceeded(sequence, timestamp, journeyId, workflow, DecidedBy.Default)
        }
        return JourneyStatus.Succeeded(journey.data)
    }
}
