package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.conformance.record.dispatchers
import org.itineradev.itinera.conformance.record.failingDispatchers
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
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
    for (step in workflow.steps) builder.step(stepOf(step, model.step(step), witness))
    val generator = workflow.idGenerator
    if (generator is IdGenerator.Returns) builder.idGenerator { _, _ -> generator.id }
    return LocalJourneys(builder.build(), model, recorders)
}

private fun stepOf(name: String, script: StepScript, witness: Witness): StepDescriptor<Sync> {
    if (script.isAsynchronous) {
        throw ModelException(
            "Kotlin hands no asynchronous step to LocalExecutor: its types refuse it"
        )
    }
    return StepDescriptor.builder(name, witness.watching(name, mockOf(script)))
        .scriptedBy(script)
        .build()
}

private class LocalJourneys(
    private val descriptor: WorkflowDescriptor<Scenario, Sync>,
    private val model: Model,
    private val recorders: Recorders,
) : Journeys {
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
        for (name in model.workflow.reporters.orEmpty()) builder.reporter(recorders.make(name))
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
