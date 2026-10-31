package org.itineradev.itinera.conformance.run

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.conformance.model.DispatcherScript
import org.itineradev.itinera.conformance.model.Dispatching
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.StepScript
import org.itineradev.itinera.conformance.record.asyncDispatchers
import org.itineradev.itinera.conformance.record.failingAsyncDispatchers
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
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
    for (step in workflow.steps) builder.step(asyncStepOf(step, model.step(step), witness))
    val generator = workflow.idGenerator
    if (generator is IdGenerator.Returns) builder.idGenerator { _, _ -> generator.id }
    return AsyncJourneys(builder.build(), model, recorders)
}

private fun asyncStepOf(name: String, script: StepScript, witness: Witness): StepDescriptor<Async> =
    asyncStep(name, witness.watchingAsync(name, asyncMockOf(script))).scriptedBy(script).build()

private class AsyncJourneys(
    private val descriptor: WorkflowDescriptor<Scenario, Async>,
    private val model: Model,
    private val recorders: Recorders,
) : Journeys {
    override fun run(times: Int): List<Journey> {
        val executor = executorOf(model.dispatching, recorders)
        return List(times) { journeyOn(executor) }
    }

    private fun journeyOn(executor: AsyncLocalExecutor): Journey =
        journeyOf(::instance, ::idOf) { runBlocking { executor.run(it) } }

    private fun instance(): WorkflowInstance<Async> {
        val builder = descriptor.instance(Scenario).initialData(model.initialData)
        for ((key, value) in model.initialGlobalMetadata) {
            builder.reportersGlobalMetadata(key, value)
        }
        for (name in model.workflow.reporters.orEmpty()) {
            builder.reporter(AsyncScenarioReporter(recorders.make(name)))
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
