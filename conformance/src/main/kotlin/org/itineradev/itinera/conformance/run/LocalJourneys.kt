package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.AdapterModel
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ProviderModel
import org.itineradev.itinera.conformance.model.ProviderScope
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.conformance.record.dispatchers
import org.itineradev.itinera.conformance.record.failingDispatchers
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.kotlin.builder

/**
 * The journeys of the workflow the scenario declares, put together with the synchronous builder,
 * each step a step mock the witness watches, run on `LocalExecutor`.
 *
 * @throws org.itineradev.itinera.workflow.Violations when the builder refuses the workflow.
 * @throws NotRunYet when the scenario scripts what the runner cannot run yet.
 */
internal fun localJourneys(model: Model, recorders: Recorders, witness: Witness): Journeys {
    val workflow = model.workflow
    val builder = builder<Scenario>(workflow.name)
    for (step in workflow.steps) builder.step(stepOf(step, model, witness))
    for (adapter in workflow.adapters) builder.inputAdapter(adapterOf(adapter))
    for (provider in workflow.providers) builder.provider(providerOf(provider))
    for (name in workflow.reporters.orEmpty()) {
        builder.reporter { recorders.make(name) }
    }
    val generator = workflow.idGenerator
    if (generator is IdGenerator.Returns) builder.idGenerator { _, _ -> generator.id }
    return LocalJourneys(builder.build(), model, recorders)
}

private fun stepOf(name: String, model: Model, witness: Witness): StepDescriptor<Sync> {
    val script = model.step(name)
    if (script.isAsynchronous) {
        throw ModelException(
            "Kotlin hands no asynchronous step to LocalExecutor: its types refuse it"
        )
    }
    val what = notRunYetIn(script)
    val factory = if (what == null) witness.watching(name, mockOf(script)) else notRunYet(what)
    val builder = StepDescriptor.builder(name, factory).scriptedBy(script)
    script.services.map(::requestOf).forEach(builder::request)
    if (script.hasRescueStep) builder.rescue(rescueOf(model.step("${name}_rescue")))
    return builder.build()
}

private fun notRunYet(what: String): SyncStepFactory = SyncStepFactory { throw NotRunYet(what) }

private fun rescueOf(script: StepScript): RescueStepDescriptor<Sync> {
    val builder = RescueStepDescriptor.builder { throw NotRunYet("rescue steps") }
    script.services.map(::requestOf).forEach(builder::request)
    return builder.build()
}

private fun adapterOf(adapter: AdapterModel): InputAdapterDescriptor<Scenario, Sync> {
    val builder =
        InputAdapterDescriptor.builder<Scenario>(adapter.name, adapter.steps.first()) { _, _ ->
            throw NotRunYet("input adapters")
        }
    adapter.steps.drop(1).forEach(builder::step)
    return builder.build()
}

private fun providerOf(provider: ProviderModel): ProviderDescriptor<Scenario, Sync> =
    providerOf(provider, classOf(provider.type))

private fun <T : Any> providerOf(
    provider: ProviderModel,
    type: Class<T>,
): ProviderDescriptor<Scenario, Sync> =
    when (provider.scope) {
        ProviderScope.JOURNEY ->
            ProviderDescriptor.builder<Scenario, T>(provider.name, provider.key, type) { _, _ ->
                    throw NotRunYet("service providers")
                }
                .servingOnly(provider)
                .build()
        ProviderScope.ATTEMPT ->
            ProviderDescriptor.perAttempt<Scenario, T>(provider.name, provider.key, type) { _, _ ->
                    throw NotRunYet("service providers")
                }
                .servingOnly(provider)
                .build()
    }

private class LocalJourneys(
    private val descriptor: WorkflowDescriptor<Scenario, Sync>,
    private val model: Model,
    private val recorders: Recorders,
) : Journeys {
    override val listing: List<ListedStep>
        get() = descriptor.listing()

    override fun run(times: Int): List<Journey> {
        val executor = executorOf(model.dispatching, recorders)
        return List(times) { journeyOn(executor) }
    }

    private fun journeyOn(executor: LocalExecutor): Journey =
        journeyOf(::instance, ::idOf) { executor.run(it) }

    private fun instance(): WorkflowInstance<Sync> {
        val builder = descriptor.instance(Scenario).initialData(model.initialData)
        for ((key, value) in model.initialGlobalMetadata) {
            builder.reportersGlobalMetadata(key, value)
        }
        return builder.create()
    }
}

private fun executorOf(dispatching: Dispatching, recorders: Recorders): LocalExecutor =
    when (dispatching) {
        Dispatching.Unstated ->
            LocalExecutor(dispatchers(recorders.make(RUNNERS_RECORDER), DispatcherScript.Delivers))
        Dispatching.Default -> LocalExecutor()
        Dispatching.FactoryFails -> LocalExecutor(failingDispatchers)
        is Dispatching.Holding ->
            LocalExecutor(dispatchers(recorders.make(dispatching.reporter), dispatching.script))
    }
