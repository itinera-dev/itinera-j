package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * Resolves what the steps of [instance]'s journey request for each attempt: first their inputs, in
 * the order each step declares them, then their services, lent by their providers.
 */
internal class Building<M : Mode>(
    private val journey: Journey,
    parts: Parts<M>,
    private val instance: WorkflowInstance<M>,
) {
    private val inputs = Inputs(journey, parts, instance)
    private val lending = Lending(journey, parts, instance)

    /**
     * What [step]'s [attempt] is built from.
     *
     * @throws JourneyAbortSignal when resolving a request aborts the journey.
     */
    @JvmSynthetic
    suspend fun got(step: StepDescriptor<M>, attempt: Attempt): Got<StepKind<M>> {
        val requests = step.requests
        val read =
            requests.filter(::asksInput).associateWith { input -> inputOf(step, attempt, input) }
        val lent =
            requests.filter(::asksService).associateWith { service ->
                lentTo(step, attempt, service)
            }
        val call =
            Call(
                RequestedBy(step.name, ByStep, StepSource(step.name, attempt)),
                mapOf(
                    Known.JOURNEY_ID to instance.journeyId,
                    Known.STEP_NAME to step.name,
                    Known.ATTEMPT to attempt,
                ),
                emptyMap(),
            )
        return Answered(requests, read + lent + journey.resolved(requests, call))
    }

    private suspend fun inputOf(
        step: StepDescriptor<M>,
        attempt: Attempt,
        input: Request<*, *>,
    ): Any? = inputs.of(step.name, attempt, input.asked as InputAsked)

    private suspend fun lentTo(
        step: StepDescriptor<M>,
        attempt: Attempt,
        service: Request<*, *>,
    ): Any = lending.of(step.name, attempt, service.asked as ServiceAsked)
}

private fun asksInput(request: Request<*, *>): Boolean = request.asked is InputAsked

private fun asksService(request: Request<*, *>): Boolean = request.asked is ServiceAsked
