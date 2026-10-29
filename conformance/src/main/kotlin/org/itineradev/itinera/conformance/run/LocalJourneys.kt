package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.PolicyModel
import org.itineradev.itinera.conformance.record.dispatchers
import org.itineradev.itinera.conformance.record.failingDispatchers
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.testing.ScriptedRun
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.kotlin.builder

/**
 * The journeys of the workflow the scenario declares, put together with the synchronous builder,
 * each step a step mock the witness watches, run on `LocalExecutor`.
 *
 * @throws org.itineradev.itinera.workflow.Violations when the builder refuses the workflow.
 */
internal fun localJourneys(
    model: Model,
    recorders: Recorders,
    witness: Witness,
    tested: Tested,
): Journeys {
    val workflow = model.workflow
    val builder = builder<Scenario>(workflow.name)
    val policies =
        workflow.policies.values.filter(::isStepPolicy).associateBy(PolicyModel::name) { policy ->
            stepPolicyOf(policy, witness)
        }
    for (step in workflow.steps) builder.step(stepOf(step, model, witness, policies))
    for (adapter in workflow.adapters) builder.inputAdapter(adapterOf(adapter))
    for (provider in workflow.providers) builder.provider(providerOf(provider, witness))
    if (model.testing.script != null) {
        builder.reporter { journeyId -> recorders.make(RUNNERS_RECORDER, journeyId) }
    }
    for (name in workflow.reporters.orEmpty()) {
        builder.reporter { journeyId -> recorders.make(name, journeyId) }
    }
    for (name in workflow.workflowPolicies.orEmpty()) {
        builder.policy(workflowPolicyOf(workflow.policy(name), witness))
    }
    for (role in workflow.roles.keys) builder.role(roleOf(role))
    val generator = workflow.idGenerator
    if (generator is IdGenerator.Returns) builder.idGenerator { _, _ -> generator.id }
    return LocalJourneys(builder.build(), model, recorders, witness, tested)
}

private class LocalJourneys(
    private val descriptor: WorkflowDescriptor<Scenario, Sync>,
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

    private fun journeyOn(executor: LocalExecutor): Journey =
        journeyOf(::instance, ::idOf) { executor.run(it) }

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

    private fun instance(): WorkflowInstance<Sync> {
        val scenario = Scenario(witness, model.workflow.failingOperations)
        if (model.testing.createsTheInstance) return tested.instance(descriptor, scenario, model)
        val builder = descriptor.instance(scenario).initialData(model.initialData)
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
