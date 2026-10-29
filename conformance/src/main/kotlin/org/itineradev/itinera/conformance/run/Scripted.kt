package org.itineradev.itinera.conformance.run

import java.util.concurrent.atomic.AtomicInteger
import org.itineradev.itinera.conformance.model.Input
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.StepAction
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.ServiceRequest
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.request.StepReporterKind
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder

/**
 * What a scripted step was given for one attempt: its inputs and its services, by key, its
 * contributor and its step reporter, of type [R], with how many steps were built from its script
 * before it, which plays the attempts' rows in turn, across journeys, as its step mock does.
 */
internal data class Received<R>(
    val inputs: Map<String, Any?>,
    val services: Map<String, Any>,
    val contributor: Contributor,
    val reporter: R,
    val builtBefore: Int,
)

/**
 * What the step [script] scripts requests, its step reporter through [reporter], and what it does
 * with what it receives before the rest of its script: it uses each service once, changes the
 * inputs the script says, then acts as the script says, first what every attempt does, in the order
 * written, then what this attempt contributes.
 */
internal class Scripted<M : Mode, R>(
    private val script: StepScript,
    private val reporter: Request<StepReporterKind<M>, R>,
) {
    private val inputs = script.inputs.associate(::requested)
    private val services = script.services.map(::requestOf)
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
        inputs.values.forEach(builder::request)
        services.forEach(builder::request)
        builder.request(contributor)
        builder.request(reporter)
        return builder
    }

    /** What [got] gives the step for one attempt. */
    fun receivedFrom(got: Got<StepKind<M>>): Received<R> =
        Received(
            inputs.mapValues { entry -> got[entry.value] },
            services.associateBy(ServiceRequest<*>::key) { service -> got[service] },
            got[contributor],
            got[reporter],
            built.getAndIncrement(),
        )

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
