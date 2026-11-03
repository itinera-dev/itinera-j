package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.AdapterModel
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ProviderModel
import org.itineradev.itinera.conformance.model.ProviderScope
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncProviderReporter
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.async.asyncPerAttemptProvider
import org.itineradev.itinera.service.async.asyncProvider
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.async.asyncInputAdapter
import org.itineradev.itinera.workflow.async.asyncRescueStep
import org.itineradev.itinera.workflow.async.asyncStep

/**
 * The asynchronous step [name] the scenario scripts, a step mock the witness watches, with its
 * policies, taken from [policies], and its rescue step.
 */
internal fun asyncStepOf(
    name: String,
    model: Model,
    witness: Witness,
    policies: Map<String, StepPolicyDescriptor<*, Async>>,
): StepDescriptor<Async> {
    val script = model.step(name)
    val scripted = Scripted(script, Requests.asyncStepReporter())
    val factory = witness.watchingAsync(name, asyncMockOf(script), scripted)
    val builder = scripted.declaredOn(asyncStep(name, factory).scriptedBy(script))
    for (policy in script.policies.orEmpty()) builder.policy(attached(policies, policy))
    if (script.hasRescueStep) builder.rescue(asyncRescueOf("${name}_rescue", model, witness))
    return builder.build()
}

private fun asyncRescueOf(
    name: String,
    model: Model,
    witness: Witness,
): RescueStepDescriptor<Async> {
    val script = model.step(name)
    val scripted = Scripted(script, Requests.asyncStepReporter())
    val factory = witness.watchingAsyncRescue(name, asyncRescueMockOf(script), scripted)
    return scripted.declaredOn(asyncRescueStep(factory)).build()
}

/** The asynchronous input adapter [adapter] scripts. */
internal fun asyncAdapterOf(adapter: AdapterModel): InputAdapterDescriptor<Scenario, Async> {
    val adapting = Adapting(adapter)
    val builder =
        asyncInputAdapter<Scenario>(adapter.name, adapter.steps.first()) { _, got ->
            adapting.supplied(got)
        }
    adapter.steps.drop(1).forEach(builder::step)
    return adapting.declaredOn(builder).build()
}

/** The asynchronous service provider [provider] scripts, its calls watched by [witness]. */
internal fun asyncProviderOf(
    provider: ProviderModel,
    witness: Witness,
): ProviderDescriptor<Scenario, Async> = asyncProviderOf(provider, witness, classOf(provider.type))

private fun <T : Any> asyncProviderOf(
    provider: ProviderModel,
    witness: Witness,
    type: Class<T>,
): ProviderDescriptor<Scenario, Async> =
    when (provider.scope) {
        ProviderScope.JOURNEY -> {
            val providing =
                journeyProviding<Async, _>(provider, witness, Requests.asyncProviderReporter())
            providing
                .declaredOn(
                    asyncProvider<Scenario, T>(provider.name, provider.key, type) { _, got ->
                        providing.providedAsync(got, type, ::performAsync)
                    }
                )
                .servingOnly(provider)
                .build()
        }
        ProviderScope.ATTEMPT -> {
            val providing =
                attemptProviding<Async, _>(provider, witness, Requests.asyncProviderReporter())
            providing
                .declaredOn(
                    asyncPerAttemptProvider<Scenario, T>(provider.name, provider.key, type) { _, got
                        ->
                        providing.providedAsync(got, type, ::performAsync)
                    }
                )
                .servingOnly(provider)
                .build()
        }
    }
