package org.itineradev.itinera.conformance.run

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.conformance.model.AdapterModel
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.PolicyModel
import org.itineradev.itinera.conformance.model.ProviderModel
import org.itineradev.itinera.conformance.model.ProviderScope
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.conformance.record.asyncDispatchers
import org.itineradev.itinera.conformance.record.failingAsyncDispatchers
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncProviderReporter
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.async.asyncPerAttemptProvider
import org.itineradev.itinera.service.async.asyncProvider
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncInputAdapter
import org.itineradev.itinera.workflow.async.asyncRescueStep
import org.itineradev.itinera.workflow.async.asyncStep

/**
 * The journeys of the workflow the scenario declares, put together with the asynchronous builder,
 * each step an asynchronous step mock the witness watches, run on `AsyncLocalExecutor`.
 *
 * @throws org.itineradev.itinera.workflow.Violations when the builder refuses the workflow.
 * @throws NotRunYet when the scenario scripts what the runner cannot run yet.
 */
internal fun asyncJourneys(model: Model, recorders: Recorders, witness: Witness): Journeys {
    if (model.onlySynchronous) {
        throw ModelException(
            "a scenario whose executor accepts only synchronous parts runs only synchronously"
        )
    }
    val workflow = model.workflow
    val builder = asyncBuilder<Scenario>(workflow.name)
    val policies =
        workflow.policies.values.filter(::isStepPolicy).associateBy(PolicyModel::name) { policy ->
            asyncStepPolicyOf(policy, witness)
        }
    for (step in workflow.steps) builder.step(asyncStepOf(step, model, witness, policies))
    for (adapter in workflow.adapters) builder.inputAdapter(asyncAdapterOf(adapter))
    for (provider in workflow.providers) builder.provider(asyncProviderOf(provider, witness))
    for (name in workflow.reporters.orEmpty()) {
        builder.reporter { journeyId -> AsyncScenarioReporter(recorders.make(name, journeyId)) }
    }
    for (name in workflow.workflowPolicies.orEmpty()) {
        builder.policy(asyncWorkflowPolicyOf(workflow.policy(name), witness))
    }
    for (role in workflow.roles.keys) builder.role(roleOf(role))
    val generator = workflow.idGenerator
    if (generator is IdGenerator.Returns) builder.idGenerator { _, _ -> generator.id }
    return AsyncJourneys(builder.build(), model, recorders, witness)
}

private fun asyncStepOf(
    name: String,
    model: Model,
    witness: Witness,
    policies: Map<String, StepPolicyDescriptor<*, Async>>,
): StepDescriptor<Async> {
    val script = model.step(name)
    val scripted = Scripted(script, Requests.asyncStepReporter())
    val what = notRunYetIn(script)
    val factory =
        if (what == null) witness.watchingAsync(name, asyncMockOf(script), scripted)
        else asyncNotRunYet(what)
    val builder = scripted.declaredOn(asyncStep(name, factory).scriptedBy(script))
    for (policy in script.policies.orEmpty()) builder.policy(attached(policies, policy))
    if (script.hasRescueStep) builder.rescue(asyncRescueOf(model.step("${name}_rescue")))
    return builder.build()
}

private fun asyncNotRunYet(what: String): AsyncStepFactory = AsyncStepFactory {
    throw NotRunYet(what)
}

private fun asyncRescueOf(script: StepScript): RescueStepDescriptor<Async> {
    val builder = asyncRescueStep { throw NotRunYet("rescue steps") }
    script.services.map(::requestOf).forEach(builder::request)
    return builder.build()
}

private fun asyncAdapterOf(adapter: AdapterModel): InputAdapterDescriptor<Scenario, Async> {
    val adapting = Adapting(adapter)
    val builder =
        asyncInputAdapter<Scenario>(adapter.name, adapter.steps.first()) { _, got ->
            adapting.supplied(got)
        }
    adapter.steps.drop(1).forEach(builder::step)
    return adapting.declaredOn(builder).build()
}

private fun asyncProviderOf(
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

private class AsyncJourneys(
    private val descriptor: WorkflowDescriptor<Scenario, Async>,
    private val model: Model,
    private val recorders: Recorders,
    private val witness: Witness,
) : Journeys {
    override val listing: List<ListedStep>
        get() = descriptor.listing()

    override fun run(times: Int): List<Journey> {
        val executor = executorOf(model.dispatching, recorders)
        return List(times) { journeyOn(executor) }
    }

    private fun journeyOn(executor: AsyncLocalExecutor): Journey =
        journeyOf(::instance, ::idOf) { runBlocking { executor.run(it) } }

    private fun instance(): WorkflowInstance<Async> {
        val builder =
            descriptor
                .instance(Scenario(witness, model.workflow.failingOperations))
                .initialData(model.initialData)
        for ((key, value) in model.initialGlobalMetadata) {
            builder.reportersGlobalMetadata(key, value)
        }
        return builder.create()
    }
}

private fun executorOf(dispatching: Dispatching, recorders: Recorders): AsyncLocalExecutor =
    when (dispatching) {
        Dispatching.Unstated ->
            AsyncLocalExecutor(
                asyncDispatchers(
                    AsyncScenarioReporter(recorders.make(RUNNERS_RECORDER)),
                    DispatcherScript.Delivers,
                )
            )
        Dispatching.Default -> AsyncLocalExecutor()
        Dispatching.FactoryFails -> AsyncLocalExecutor(failingAsyncDispatchers)
        is Dispatching.Holding ->
            AsyncLocalExecutor(
                asyncDispatchers(
                    AsyncScenarioReporter(recorders.make(dispatching.reporter)),
                    dispatching.script,
                )
            )
    }
