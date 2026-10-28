package org.itineradev.itinera.engine

import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The violations of the services the steps of [descriptor] request, step by step: each key a step
 * requests twice, then each key no provider serves for it, or whose provider supplies another type.
 */
internal fun serviceViolations(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    descriptor.steps.flatMap { step -> serviceViolations(step, descriptor.providers) }

private fun serviceViolations(
    step: StepDescriptor<*>,
    providers: List<ProviderDescriptor<*, *>>,
): List<Violation> {
    val services = step.requests.map(::askedBy).filterIsInstance<ServiceAsked>()
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
        provider.type != service.type ->
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
