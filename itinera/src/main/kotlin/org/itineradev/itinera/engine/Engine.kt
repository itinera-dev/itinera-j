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
import org.itineradev.itinera.policy.WorkflowHook
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
        return JourneyResult(instance.journeyId, statusOf(journey, instance, started.policies))
    }

    private suspend fun statusOf(
        journey: Journey,
        instance: WorkflowInstance<M>,
        policies: List<BuiltPolicy<WorkflowHook>>,
    ): JourneyStatus =
        try {
            ran(journey, instance, policies)
        } catch (signal: JourneyAbortSignal) {
            journey.deliverAborted(signal.abort)
            JourneyStatus.Aborted(signal.abort)
        }

    /**
     * Runs the steps in their order until one is given up or a hook ends the journey early, then
     * calls the workflow hook for how the journey ends, with no step current, before the journey's
     * last event.
     */
    private suspend fun ran(
        journey: Journey,
        instance: WorkflowInstance<M>,
        policies: List<BuiltPolicy<WorkflowHook>>,
    ): JourneyStatus {
        val keys = journey.data.keys
        journey.emit { JourneyStarted(sequence, timestamp, journeyId, workflow, keys) }
        val hooks = Hooks(journey, parts, instance)
        val attempting = Attempting(journey, parts, Building(journey, parts, instance))
        val attempts = Attempts(journey, parts, attempting, hooks)
        val early = instance.descriptor.steps.firstNotNullOfOrNull { step -> attempts.of(step) }
        journey.step = null
        if (early is Early.Failed) {
            hooks.ofWorkflow(policies, WorkflowHook.ON_WORKFLOW_FAILURE)
            journey.emit {
                JourneyFailed(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    early.step,
                    early.failure.details,
                )
            }
            return JourneyStatus.Failed(early.failure, journey.data)
        }
        hooks.ofWorkflow(policies, WorkflowHook.ON_WORKFLOW_SUCCESS)
        val decidedBy = (early as? Early.Finished)?.by ?: DecidedBy.Default
        journey.emit { JourneySucceeded(sequence, timestamp, journeyId, workflow, decidedBy) }
        return JourneyStatus.Succeeded(journey.data)
    }
}
