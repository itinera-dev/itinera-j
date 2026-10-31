package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Violation

/**
 * Reads the service providers of a workflow class in [mode], its [markedMethods] marked
 * `@Provider`, recording each mistake in [problems].
 */
internal class ProviderMethods<M : Mode>(
    private val mode: M,
    private val calls: ProviderCalls<M>,
    private val problems: Problems,
) {
    private val requests = ProviderRequests(calls, problems)

    /** The providers [type] declares, leaving out those with a mistake. */
    fun <W> of(type: Class<*>): List<ProviderDescriptor<W, M>> =
        markedMethods(type, Provider::class.java).mapNotNull(::provider).toList()

    private fun provider(method: Method): ProviderDescriptor<Any?, M>? {
        val marked = method.getAnnotation(Provider::class.java)
        val function = functionOf(method)
        val name = marked.name.ifEmpty { function.name }
        val inMode = function.isSuspend == (mode == Async)
        if (!inMode) problems += Violation.ModeNotAccepted("service provider $name", mode)
        val stepsNamed = stepsNamed(method, marked)
        val asks = requests.of(method, function)
        val returns = problems.returns(method, function, Provided::class.java)
        val service = serviceOf(method, function)
        val opened = problems.opened(method.declaringClass, method)
        val scoped = asks == null || marked.perAttempt || inJourneyScope(name, asks)
        val usable = inMode && stepsNamed && returns && opened && scoped
        return if (usable && asks != null && service != null)
            described(method, name, function.name, marked, service, asks)
        else null
    }

    private fun stepsNamed(method: Method, marked: Provider): Boolean {
        val empty = marked.steps.any(String::isEmpty)
        if (empty) problems.misdeclared(method.toString(), "names a step with an empty name")
        return !empty
    }

    /** The class of the service [method] provides, the argument of its `Provided`. */
    private fun serviceOf(method: Method, function: KotlinFunction): Class<*>? {
        val service = function.returnArguments.singleOrNull()
        if (service == null && Provided::class.java.isAssignableFrom(function.returnType))
            problems.misdeclared(
                method.toString(),
                "returns Provided of no class, which names no service type: " +
                    "write the service's type in Provided",
            )
        return service
    }

    /** Whether [asks] holds only what a provider of journey scope named [name] may request. */
    private fun inJourneyScope(
        name: String,
        asks: List<Request<AttemptProviderKind<M>, *>>,
    ): Boolean {
        val attempted = asks.map(::askedBy).any(::isOfTheAttempt)
        if (attempted) problems += Violation.AttemptRequestInJourneyScope(ProviderName.of(name))
        return !attempted
    }

    private fun described(
        method: Method,
        name: String,
        named: String,
        marked: Provider,
        service: Class<*>,
        asks: List<Request<AttemptProviderKind<M>, *>>,
    ): ProviderDescriptor<Any?, M> {
        val key = marked.key.ifEmpty { named }
        val type: Class<Any> = returned(service)
        return if (marked.perAttempt)
            built(
                ProviderBuilder.attempt(name, key, type, calls.of(ProviderMethod(method, asks))),
                marked,
                asks,
            )
        else {
            val journeyAsks: List<Request<JourneyProviderKind<M>, *>> = returned(asks)
            built(
                ProviderBuilder.journey(
                    name,
                    key,
                    type,
                    calls.of(ProviderMethod(method, journeyAsks)),
                ),
                marked,
                journeyAsks,
            )
        }
    }
}

private fun <M : Mode, K> built(
    builder: ProviderBuilder<Any?, M, K>,
    marked: Provider,
    asks: List<Request<K, *>>,
): ProviderDescriptor<Any?, M> {
    marked.steps.forEach(builder::step)
    asks.forEach(builder::request)
    return builder.build()
}
