package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.AdapterModel
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ProviderModel
import org.itineradev.itinera.conformance.model.ProviderScope
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.kotlin.builder

/**
 * The synchronous step [name] the scenario scripts, a step mock the witness watches, with its
 * policies, taken from [policies], and its rescue step.
 */
internal fun stepOf(
    name: String,
    model: Model,
    witness: Witness,
    policies: Map<String, StepPolicyDescriptor<*, Sync>>,
): StepDescriptor<Sync> {
    val script = model.step(name)
    if (script.isAsynchronous) {
        throw ModelException(
            "Kotlin hands no asynchronous step to LocalExecutor: its types refuse it"
        )
    }
    val scripted = Scripted(script, Requests.stepReporter())
    val factory = witness.watching(name, mockOf(script), scripted)
    val builder = scripted.declaredOn(StepDescriptor.builder(name, factory).scriptedBy(script))
    for (policy in script.policies.orEmpty()) builder.policy(attached(policies, policy))
    if (script.hasRescueStep) builder.rescue(rescueOf("${name}_rescue", model, witness))
    return builder.build()
}

private fun rescueOf(name: String, model: Model, witness: Witness): RescueStepDescriptor<Sync> {
    val script = model.step(name)
    val scripted = Scripted(script, Requests.stepReporter())
    val factory = witness.watchingRescue(name, rescueMockOf(script), scripted)
    return scripted.declaredOn(RescueStepDescriptor.builder(factory)).build()
}

/** The synchronous input adapter [adapter] scripts. */
internal fun adapterOf(adapter: AdapterModel): InputAdapterDescriptor<Scenario, Sync> {
    if (adapter.isAsynchronous) {
        throw ModelException(
            "Kotlin hands no asynchronous input adapter to LocalExecutor: its types refuse it"
        )
    }
    val adapting = Adapting(adapter)
    val builder =
        InputAdapterDescriptor.builder<Scenario>(adapter.name, adapter.steps.first()) { _, got ->
            adapting.supplied(got)
        }
    adapter.steps.drop(1).forEach(builder::step)
    return adapting.declaredOn(builder).build()
}

/** The synchronous service provider [provider] scripts, its calls watched by [witness]. */
internal fun providerOf(
    provider: ProviderModel,
    witness: Witness,
): ProviderDescriptor<Scenario, Sync> {
    if (provider.isAsynchronous) {
        throw ModelException(
            "Kotlin hands no asynchronous service provider to LocalExecutor: its types refuse it"
        )
    }
    return providerOf(provider, witness, classOf(provider.type))
}

private fun <T : Any> providerOf(
    provider: ProviderModel,
    witness: Witness,
    type: Class<T>,
): ProviderDescriptor<Scenario, Sync> =
    when (provider.scope) {
        ProviderScope.JOURNEY -> {
            val providing =
                journeyProviding<Sync, _>(provider, witness, Requests.providerReporter())
            providing
                .declaredOn(
                    ProviderDescriptor.builder<Scenario, T>(provider.name, provider.key, type) {
                        _,
                        got ->
                        providing.provided(got, type, ::perform)
                    }
                )
                .servingOnly(provider)
                .build()
        }
        ProviderScope.ATTEMPT -> {
            val providing =
                attemptProviding<Sync, _>(provider, witness, Requests.providerReporter())
            providing
                .declaredOn(
                    ProviderDescriptor.perAttempt<Scenario, T>(provider.name, provider.key, type) {
                        _,
                        got ->
                        providing.provided(got, type, ::perform)
                    }
                )
                .servingOnly(provider)
                .build()
        }
    }
