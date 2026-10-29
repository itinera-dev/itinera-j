package org.itineradev.itinera.workflow.checker

/**
 * The mistakes in the services the steps of [workflow] and their rescue steps request: each key one
 * requests twice, each service no provider serves it, and each a provider serves of another type.
 */
internal fun serviceMistakes(workflow: ReadWorkflow): List<Finding> =
    workflow.steps.flatMap(::requesting).flatMap { step ->
        services(step, workflow, workflow.origin)
    }

/** A step, or a rescue step, by its name and the services it requests. */
private class Requesting(val name: String, val services: List<ServiceAsked>)

private fun requesting(step: DeclaredStep): List<Requesting> {
    val requesting = Requesting(step.name, step.asks.filterIsInstance<ServiceAsked>())
    val rescue = step.rescue ?: return listOf(requesting)
    return listOf(
        requesting,
        Requesting("${step.name}_rescue", rescue.filterIsInstance<ServiceAsked>()),
    )
}

private fun services(step: Requesting, workflow: ReadWorkflow, origin: Any): List<Finding> =
    shared(step.services.map(ServiceAsked::key)).map { key ->
        requestedTwice(step.name, key, origin)
    } +
        step.services.distinctBy(ServiceAsked::key).mapNotNull { service ->
            unserved(step.name, service, workflow.providers, origin)
        }

private fun requestedTwice(step: String, key: String, origin: Any): Finding =
    Named("service requested twice", "step $step requests service $key twice", origin)

private fun unserved(
    step: String,
    service: ServiceAsked,
    providers: List<ReadProvider>,
    origin: Any,
): Finding? {
    val provider =
        providers.find { provider -> servesStep(provider, service.key, step) }
            ?: providers.find { provider -> servesEveryStep(provider, service.key) }
    return when {
        provider == null ->
            Named(
                "service not provided",
                "no service provider serves service ${service.key} to step $step",
                origin,
            )
        provider.service != service.type ->
            Named(
                "wrong service type",
                "service provider ${provider.name} serves step $step another type than it " +
                    "requests as ${service.key}",
                origin,
            )
        else -> null
    }
}

private fun servesStep(provider: ReadProvider, key: String, step: String): Boolean =
    provider.key == key && step in provider.steps

private fun servesEveryStep(provider: ReadProvider, key: String): Boolean =
    provider.key == key && provider.steps.isEmpty()
