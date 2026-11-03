package org.itineradev.itinera.workflow.checker

/**
 * The mistakes in how the service providers of [workflow] are attached, to [steps]: each name
 * several share, each step or each key of no step that several attachments serve, and each provider
 * attached to a step the workflow does not have, once.
 */
internal fun providerMistakes(workflow: ReadWorkflow, steps: Set<String>): List<Finding> {
    val origin = workflow.origin
    return shared(workflow.providers.map(ReadProvider::name)).map { name ->
        duplicateProviderName(name, origin)
    } +
        providedTwice(workflow.providers, steps, origin) +
        workflow.providers
            .flatMap { provider -> forUnknownSteps(provider, steps, origin) }
            .distinct()
}

private fun duplicateProviderName(name: String, origin: Any): Finding =
    Named("duplicate provider name", "two service providers are named $name", origin)

private fun forUnknownSteps(
    provider: ReadProvider,
    steps: Set<String>,
    origin: Any,
): List<Finding> =
    provider.steps.filterNot(steps::contains).map { step ->
        providerForUnknownStep(provider.name, step, origin)
    }

private fun providerForUnknownStep(provider: String, step: String, origin: Any): Finding =
    Named(
        "service provider for unknown step",
        "service provider $provider names step $step, which the workflow does not have",
        origin,
    )

/** What a provider's attachment serves: a key, for one step or, when [step] is `null`, for all. */
private data class Serving(val key: String, val step: String?)

/** One provider attached once to what it serves. */
private class Attachment(val serving: Serving, val provider: String)

private fun providedTwice(
    providers: List<ReadProvider>,
    steps: Set<String>,
    origin: Any,
): List<Finding> =
    providers
        .flatMap { provider -> attachments(provider, steps) }
        .groupBy(Attachment::serving, Attachment::provider)
        .filterValues(::isSharedList)
        .map { (serving, attached) -> servedTwice(serving, attached, origin) }

private fun servedTwice(serving: Serving, providers: List<String>, origin: Any): Finding {
    val served = if (serving.step == null) "every step" else "step ${serving.step}"
    return Named(
        "service provided twice",
        "service providers ${providers.joinToString(", ")} all serve service ${serving.key} to " +
            served,
        origin,
    )
}

private fun attachments(provider: ReadProvider, steps: Set<String>): List<Attachment> =
    if (provider.steps.isEmpty()) listOf(Attachment(Serving(provider.key, null), provider.name))
    else provider.steps.filter(steps::contains).map { step -> attachment(provider, step) }

private fun attachment(provider: ReadProvider, step: String): Attachment =
    Attachment(Serving(provider.key, step), provider.name)
