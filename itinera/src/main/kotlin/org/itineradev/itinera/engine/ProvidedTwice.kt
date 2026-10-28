package org.itineradev.itinera.engine

import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Violation

/**
 * Each step of [steps], or each key of no step, that several attachments of [providers] serve, as
 * `service provided twice`; attachments to a step the workflow does not have count for none.
 */
internal fun providedTwice(
    providers: List<ProviderDescriptor<*, *>>,
    steps: Set<StepName>,
): List<Violation> =
    providers
        .flatMap { provider -> attachments(provider, steps) }
        .groupBy(Attachment::serving, Attachment::provider)
        .filterValues(::isProvidedTwice)
        .map(::providedTwice)

/** What a provider's attachment serves: a key, for one step or, when [step] is `null`, for all. */
private data class Serving(val key: String, val step: StepName?)

/** One provider attached once to what it serves. */
private class Attachment(val serving: Serving, val provider: ProviderName)

/** Each attachment of [provider] to one of [steps], or its one attachment to no step. */
private fun attachments(
    provider: ProviderDescriptor<*, *>,
    steps: Set<StepName>,
): List<Attachment> =
    if (provider.steps.isEmpty()) listOf(attachment(provider, null))
    else provider.steps.filter(steps::contains).map { step -> attachment(provider, step) }

private fun attachment(provider: ProviderDescriptor<*, *>, step: StepName?): Attachment =
    Attachment(Serving(provider.key, step), provider.name)

private fun isProvidedTwice(providers: List<ProviderName>): Boolean = isShared(providers.size)

private fun providedTwice(served: Map.Entry<Serving, List<ProviderName>>): Violation =
    Violation.ServiceProvidedTwice(served.key.key, served.key.step, served.value)
