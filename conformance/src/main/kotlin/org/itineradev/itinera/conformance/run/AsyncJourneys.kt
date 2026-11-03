package org.itineradev.itinera.conformance.run

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.PolicyModel
import org.itineradev.itinera.conformance.record.asyncDispatchers
import org.itineradev.itinera.conformance.record.failingAsyncDispatchers
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.executor.testing.ScriptedRun
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder

/**
 * The journeys of the workflow the scenario declares, put together with the asynchronous builder,
 * each step an asynchronous step mock the witness watches, run on `AsyncLocalExecutor`.
 *
 * @throws org.itineradev.itinera.workflow.Violations when the builder refuses the workflow.
 */
internal fun asyncJourneys(
    model: Model,
    recorders: Recorders,
    witness: Witness,
    tested: Tested,
): Journeys {
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
    if (model.testing.script != null) {
        builder.reporter { journeyId ->
            AsyncScenarioReporter(recorders.make(RUNNERS_RECORDER, journeyId))
        }
    }
    for (name in workflow.reporters.orEmpty()) {
        builder.reporter { journeyId -> AsyncScenarioReporter(recorders.make(name, journeyId)) }
    }
    for (name in workflow.workflowPolicies.orEmpty()) {
        builder.policy(asyncWorkflowPolicyOf(workflow.policy(name), witness))
    }
    for (role in workflow.roles.keys) builder.role(roleOf(role))
    val generator = workflow.idGenerator
    if (generator is IdGenerator.Returns) builder.idGenerator { _, _ -> generator.id }
    return AsyncJourneys(builder.build(), model, recorders, witness, tested)
}

private class AsyncJourneys(
    private val descriptor: WorkflowDescriptor<Scenario, Async>,
    private val model: Model,
    private val recorders: Recorders,
    private val witness: Witness,
    private val tested: Tested,
) : Journeys {
    override val listing: List<ListedStep>
        get() = descriptor.listing()

    override fun run(times: Int): List<Journey> {
        val executor = executorOf(model.dispatching, recorders)
        return List(times) { journeyOn(executor) }
    }

    private fun journeyOn(executor: AsyncLocalExecutor): Journey =
        journeyOf(::instance, ::idOf) { runBlocking { executor.run(it) } }

    override fun runScripted(): ScriptedJourney {
        var run: ScriptedRun? = null
        val journey =
            journeyOf(::instance, ::idOf) { instance ->
                val ran = scriptedRunOf(instance, model.testing)
                run = ran
                ran.result
            }
        return ScriptedJourney(journey, run)
    }

    private fun instance(): WorkflowInstance<Async> {
        val scenario = Scenario(witness, model.workflow.failingOperations)
        if (model.testing.createsTheInstance)
            return tested.asyncInstance(descriptor, scenario, model)
        val builder = descriptor.instance(scenario).initialData(model.initialData)
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
