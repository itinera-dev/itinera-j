package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Input
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.ServiceRequest
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder

/** What a scripted step was given for one attempt: its inputs and its services, by key. */
internal data class Received(val inputs: Map<String, Any?>, val services: Map<String, Any>)

/**
 * What the step [script] scripts requests, and what it does with what it receives before the rest
 * of its script: it uses each service once, and changes the inputs the script says.
 */
internal class Scripted(private val script: StepScript) {
    private val inputs = script.inputs.associate(::requested)
    private val services = script.services.map(::requestOf)

    /** Declares the step's inputs on [builder], then its services, each in the order written. */
    fun <M : Mode> declaredOn(builder: StepBuilder<M>): StepBuilder<M> {
        inputs.values.forEach(builder::request)
        services.forEach(builder::request)
        return builder
    }

    /** What [got] gives the step for one attempt. */
    fun <M : Mode> receivedFrom(got: Got<StepKind<M>>): Received =
        Received(
            inputs.mapValues { entry -> got[entry.value] },
            services.associateBy(ServiceRequest<*>::key) { service -> got[service] },
        )

    /**
     * Uses each service the step received, whose failure escapes, then changes its inputs in place.
     */
    fun act(received: Received) {
        received.services.values.forEach(::use)
        script.inputChanges.forEach { (key, to) -> changeInPlace(received.inputs[key], to) }
    }
}

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

private fun changeInPlace(received: Any?, to: Any?) {
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
