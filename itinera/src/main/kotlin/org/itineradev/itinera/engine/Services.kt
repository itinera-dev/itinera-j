package org.itineradev.itinera.engine

import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.boxed
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The violations of the services the steps of [descriptor] request, step by step, each followed by
 * its rescue step: each key a step requests twice, then each key no provider serves for it, or
 * whose provider supplies another type.
 */
internal fun serviceViolations(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    descriptor.steps.flatMap(::requesting).flatMap { step ->
        serviceViolations(step, descriptor.providers)
    }

/** The services [requests] ask for, in order. */
internal fun servicesAskedBy(requests: List<Request<*, *>>): List<ServiceAsked> =
    requests.map(::askedBy).filterIsInstance<ServiceAsked>()

/** A step, or a rescue step, by its name and what it requests. */
private class Requesting(val name: StepName, val requests: List<Request<*, *>>)

private fun requesting(step: StepDescriptor<*>): List<Requesting> {
    val rescue = step.rescue
    val requesting = Requesting(step.name, step.requests)
    return if (rescue == null) listOf(requesting)
    else listOf(requesting, Requesting(rescueOf(step.name), rescue.requests))
}

private fun serviceViolations(
    step: Requesting,
    providers: List<ProviderDescriptor<*, *>>,
): List<Violation> {
    val services = servicesAskedBy(step.requests)
    return shared(services.map(ServiceAsked::key)).map { key -> requestedTwice(step.name, key) } +
        services.distinctBy(ServiceAsked::key).mapNotNull { service ->
            unserved(step.name, service, providers)
        }
}

/** Why [service] of [step] is not served, or `null` when its provider supplies its type. */
private fun unserved(
    step: StepName,
    service: ServiceAsked,
    providers: List<ProviderDescriptor<*, *>>,
): Violation? {
    val provider = servingOf(step, service.key, providers)
    return when {
        provider == null -> Violation.ServiceNotProvided(step, service.key)
        boxed(provider.type) != boxed(service.type) ->
            Violation.WrongServiceType(step, service.key, provider.name)
        else -> null
    }
}

/**
 * The provider that serves [step] for [key]: the one of [key] attached to it, or else the one of
 * [key] attached to no step.
 */
internal fun <P : ProviderDescriptor<*, *>> servingOf(
    step: StepName,
    key: String,
    providers: List<P>,
): P? =
    providers.find { provider -> isAttachedTo(provider, step, key) }
        ?: providers.find { provider -> servesEveryStep(provider, key) }

private fun isAttachedTo(provider: ProviderDescriptor<*, *>, step: StepName, key: String): Boolean =
    provider.key == key && step in provider.steps

private fun servesEveryStep(provider: ProviderDescriptor<*, *>, key: String): Boolean =
    provider.key == key && provider.steps.isEmpty()

private fun requestedTwice(step: StepName, key: String): Violation =
    Violation.ServiceRequestedTwice(step, key)
