package org.itineradev.itinera.conformance.run

import java.util.concurrent.atomic.AtomicInteger
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.Input
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.RescuedStepRequest
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.StepAction
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.request.ServiceRequest
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.request.StepReporterKind
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.RescueStepBuilder
import org.itineradev.itinera.workflow.StepBuilder

/**
 * What a scripted step was given for one attempt: its inputs and its services, by key, its
 * contributor and its step reporter, of type [R], with how many steps were built from its script
 * before it, which plays the attempts' rows in turn, across journeys, as its step mock does, and,
 * for a rescue step, what it received of the step it rescues, by what it requested.
 */
internal data class Received<R>(
    val inputs: Map<String, Any?>,
    val services: Map<String, Any>,
    val contributor: Contributor,
    val reporter: R,
    val builtBefore: Int,
    val ofRescuedStep: Map<HookRequest, Any?>,
)

/**
 * What the step or rescue step [script] scripts requests, its step reporter through [reporter], and
 * what it does with what it receives before the rest of its script: it uses each service once,
 * changes the inputs the script says, then acts as the script says, first what every attempt does,
 * in the order written, then what this attempt contributes.
 */
internal class Scripted<M : Mode, R>(
    private val script: StepScript,
    private val reporter: Request<StepReporterKind<M>, R>,
) {
    private val inputs = script.inputs.associate(::requested)
    private val services = script.services.map(::requestOf)
    private val ofRescuedStep: Map<HookRequest, Request<RescueKind<M>, *>> =
        script.ofRescuedStep.associate(::rescuedRequested)
    private val contributor = Requests.contributor()
    private val built = AtomicInteger()

    /** Whether the step catches what its emit calls throw and carries on. */
    val ignoresEmitFailures: Boolean
        get() = script.ignoresEmitFailures

    /**
     * Declares the step's inputs on [builder], then its services, each in the order written, then
     * its contributor and its step reporter.
     */
    fun declaredOn(builder: StepBuilder<M>): StepBuilder<M> {
        declare(inputs.values, builder::request)
        return builder
    }

    /**
     * Declares the rescue step's inputs and its requests of the step it rescues on [builder],
     * together in the order written, then its services, its contributor and its step reporter.
     */
    fun declaredOn(builder: RescueStepBuilder<M>): RescueStepBuilder<M> {
        declare(writtenForRescue(), builder::request)
        return builder
    }

    private fun <K : StepKind<M>> declare(
        first: Collection<Request<K, *>>,
        request: (Request<K, *>) -> Unit,
    ) {
        first.forEach(request)
        services.forEach(request)
        if (script.declaresNoHandles) return
        request(contributor)
        request(reporter)
    }

    private fun writtenForRescue(): List<Request<RescueKind<M>, *>> =
        (inputs.values.mapIndexed(::placedInput) + script.ofRescuedStep.map(::placedRescued))
            .sortedBy(Placed<M>::place)
            .map(Placed<M>::request)

    private fun placedInput(index: Int, request: Request<BuildKind, *>): Placed<M> =
        Placed(2 * index + 1, request)

    private fun placedRescued(written: RescuedStepRequest): Placed<M> =
        Placed(2 * written.inputsBefore, ofRescuedStep.getValue(written.request))

    /** What [got] gives the step for one attempt. */
    fun receivedFrom(got: Got<StepKind<M>>): Received<R> = received(got, emptyMap())

    /** What [got] gives the rescue step for its attempt. */
    fun rescueReceivedFrom(got: Got<RescueKind<M>>): Received<R> =
        received(got, ofRescuedStep.mapValues { entry -> got[entry.value] })

    private fun <K : StepKind<M>> received(
        got: Got<K>,
        ofRescuedStep: Map<HookRequest, Any?>,
    ): Received<R> {
        if (script.declaresNoHandles) {
            throw ModelException("the cases never build a step that declares no handles")
        }
        return Received(
            inputs.mapValues { entry -> got[entry.value] },
            services.associateBy(ServiceRequest<*>::key) { service -> got[service] },
            got[contributor],
            got[reporter],
            built.getAndIncrement(),
            ofRescuedStep,
        )
    }

    /**
     * Uses each service the step received, whose failure escapes, changes its inputs in place, and
     * gives what the step then does.
     */
    fun prepared(received: Received<R>): List<StepAction> {
        received.services.values.forEach(::use)
        script.inputChanges.forEach { (key, to) -> changeInPlace(received.inputs[key], to) }
        return script.actions + contributionsOf(received.builtBefore).map(::contributing)
    }

    /**
     * What the step built after [builtBefore] others contributes, as its row says, or as the last
     * row says for a step built past them.
     */
    private fun contributionsOf(builtBefore: Int): Map<String, Any?> =
        script.attempts?.let { rows -> rowOf(rows, builtBefore) }?.contributes.orEmpty()
}

/**
 * A rescue step's [request] at its [place] in the order written: a request of the step it rescues
 * written before input n is at 2n, and input n at 2n + 1.
 */
private class Placed<M : Mode>(val place: Int, val request: Request<RescueKind<M>, *>)

private fun <M : Mode> rescuedRequested(
    written: RescuedStepRequest
): Pair<HookRequest, Request<RescueKind<M>, *>> =
    written.request to ofKind(requestOf(written.request))

private fun contributing(entry: Map.Entry<String, Any?>): StepAction =
    StepAction.Contributes(entry.key, entry.value)

private fun rowOf(rows: List<ScriptedAttempt>, builtBefore: Int): ScriptedAttempt =
    rows.getOrElse(builtBefore) { rows.last() }

private fun requested(input: Input): Pair<String, Request<BuildKind, *>> =
    input.key to
        if (input.optional) {
            Requests.optionalInput(input.key, TypeToken.of(input.type.type))
        } else {
            Requests.input(input.key, TypeToken.of(input.type.type))
        }

private fun use(service: Any) {
    if (service is Labeller) service.label()
    if (service is Meter) service.reading()
}

/** Changes [received], a list or a map, to the content of [to], in place. */
internal fun changeInPlace(received: Any?, to: Any?) {
    if (received is MutableList<*> && to is List<*>) replaced(received, to)
    if (received is MutableMap<*, *> && to is Map<*, *>) replaced(received, to)
}

@Suppress("UNCHECKED_CAST")
private fun replaced(received: MutableList<*>, to: List<*>) {
    (received as MutableList<Any?>).apply { clear() }.addAll(to)
}

@Suppress("UNCHECKED_CAST")
private fun replaced(received: MutableMap<*, *>, to: Map<*, *>) {
    (received as MutableMap<Any?, Any?>).apply { clear() }.putAll(to)
}
