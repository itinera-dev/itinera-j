package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByRescueStep
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * Resolves what the steps of [instance]'s journey, and their rescue steps, request for each
 * attempt: first their data, in the order each declares it, then their services, lent by their
 * providers.
 */
internal class Building<M : Mode>(
    private val journey: Journey,
    parts: Parts<M>,
    private val instance: WorkflowInstance<M>,
) {
    private val inputs = Inputs(journey, parts, instance)
    private val lending = Lending(journey, parts, instance)

    /**
     * What [step]'s [attempt] is built from, the [handles] made for the attempt among it.
     *
     * @throws JourneyAbortSignal when resolving a request aborts the journey.
     */
    @JvmSynthetic
    suspend fun got(
        step: StepDescriptor<M>,
        attempt: Attempt,
        handles: Map<Handle, Any>,
    ): Got<StepKind<M>> =
        answered(step.name, attempt, step.requests, handles) { asked ->
            inputs.of(step.name, attempt, inputOf(asked))
        }

    /**
     * What [rescue], the rescue step of [step], is built from, once [step]'s failed attempt left
     * [left], the [handles] made for its attempt among it: its inputs, and what it requests from
     * the step it rescues, from what the attempt left.
     *
     * @throws JourneyAbortSignal when resolving a request aborts the journey.
     */
    @JvmSynthetic
    suspend fun rescuing(
        step: StepDescriptor<M>,
        rescue: RescueStepDescriptor<M>,
        left: Left,
        handles: Map<Handle, Any>,
    ): Got<RescueKind<M>> {
        val name = rescueOf(step.name)
        val acting = ActingOn(left, name, ByRescueStep)
        return answered(name, FIRST, rescue.requests, handles) { asked ->
            when (asked) {
                is FromStepAsked ->
                    journey.fromStep(
                        left.contributed,
                        asked.key,
                        asked.type,
                        asked.required,
                        requestedBy(name, FIRST),
                    )
                is ReasonAsked -> journey.reasonFor(acting, asked)
                is ErrorAsked -> journey.errorFor(acting, asked)
                is InputAsked -> inputs.of(name, FIRST, asked)
            }
        }
    }

    /**
     * What [attempt] of the step or rescue step [name] is built from: what [data] gives for each of
     * its [requests] for data, in order, then each service lent to it, then what the executor knows
     * and the [handles].
     */
    private suspend fun <K> answered(
        name: StepName,
        attempt: Attempt,
        requests: List<Request<K, *>>,
        handles: Map<Handle, Any>,
        data: suspend (BuiltWithAsked) -> Any?,
    ): Got<K> {
        val read =
            requests.filter(::asksData).associateWith { request ->
                data(request.asked as BuiltWithAsked)
            }
        val lent =
            requests.filter(::asksService).associateWith { service ->
                lending.of(name, attempt, service.asked as ServiceAsked)
            }
        val call =
            Call(
                requestedBy(name, attempt),
                mapOf(
                    Known.JOURNEY_ID to instance.journeyId,
                    Known.STEP_NAME to name,
                    Known.ATTEMPT to attempt,
                ),
                handles,
            )
        return Answered(requests, read + lent + journey.resolved(requests, call))
    }
}

private fun requestedBy(name: StepName, attempt: Attempt): RequestedBy =
    RequestedBy(name, ByStep, StepSource(name, attempt))

/** Whether [request] asks for data a step or rescue step is built with, besides its services. */
private fun asksData(request: Request<*, *>): Boolean = request.asked is BuiltWithAsked

/** The input [asked] stands for, the only data a step requests besides its services. */
private fun inputOf(asked: BuiltWithAsked): InputAsked =
    when (asked) {
        is InputAsked -> asked
        is FromStepAsked,
        is ReasonAsked,
        is ErrorAsked -> throw AssertionError("A step cannot request $asked.")
    }

private fun asksService(request: Request<*, *>): Boolean = request.asked is ServiceAsked
