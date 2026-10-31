package org.itineradev.itinera.engine

import org.itineradev.itinera.service.AttemptProviderDescriptor
import org.itineradev.itinera.service.JourneyProviderDescriptor
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The violations of how the service providers of [descriptor] are put together: each name several
 * share, each step or each key of no step that several attachments serve, each provider attached to
 * a step the workflow does not have, once, and each provider of journey scope that requests the
 * step's name or the attempt.
 */
internal fun providerViolations(descriptor: WorkflowDescriptor<*, *>): List<Violation> {
    val steps = descriptor.steps.map(StepDescriptor<*>::name).toSet()
    return shared(descriptor.providers.map(ProviderDescriptor<*, *>::name))
        .map(Violation::DuplicateProviderName) +
        providedTwice(descriptor.providers, steps) +
        descriptor.providers.flatMap { provider -> forUnknownSteps(provider, steps) }.distinct() +
        descriptor.providers.filter(::requestsTheAttempt).map(::attemptInJourneyScope)
}

/** How [provider] is called, whatever its scope. */
internal fun callOf(provider: ProviderDescriptor<*, *>): ProviderCall<*, *, *, *>? =
    when (provider) {
        is JourneyProviderDescriptor -> provider.call
        is AttemptProviderDescriptor -> provider.call
    }

private fun forUnknownSteps(
    provider: ProviderDescriptor<*, *>,
    steps: Set<StepName>,
): List<Violation> =
    provider.steps.filterNot(steps::contains).map { step -> forUnknownStep(provider.name, step) }

private fun forUnknownStep(provider: ProviderName, step: StepName): Violation =
    Violation.ServiceProviderForUnknownStep(provider, step)

private fun requestsTheAttempt(provider: ProviderDescriptor<*, *>): Boolean =
    provider is JourneyProviderDescriptor && provider.requests.map(::askedBy).any(::isOfTheAttempt)

/** Whether [asked] is what only a provider of attempt scope receives. */
internal fun isOfTheAttempt(asked: Asked): Boolean =
    asked == Known.STEP_NAME || asked == Known.ATTEMPT

private fun attemptInJourneyScope(provider: ProviderDescriptor<*, *>): Violation =
    Violation.AttemptRequestInJourneyScope(provider.name)
