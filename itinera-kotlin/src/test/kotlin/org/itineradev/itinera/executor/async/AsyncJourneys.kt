package org.itineradev.itinera.executor.async

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.instance.async.AsyncInstanceBuilder
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncStep

/** Workflows and reporters for the tests of the asynchronous executor. */
object AsyncJourneys {
    /** The workflow object of every workflow the tests build. */
    class Shop

    /** A step whose every attempt runs [run]. */
    fun running(name: String, run: AsyncStepRun): StepBuilder<Async> = asyncStep(name) { run }

    fun succeeding(name: String): StepBuilder<Async> = running(name) { Outcome.success() }

    /** The instance of a workflow of [steps], in order. */
    fun journeyOf(vararg steps: StepBuilder<Async>): AsyncInstanceBuilder =
        steps.fold(asyncBuilder<Shop>("shop"), ::withStep).build().instance(Shop())

    /** The instance of a workflow of one step, `pack`, that succeeds. */
    fun packing(): AsyncInstanceBuilder = journeyOf(succeeding("pack"))

    private fun withStep(
        workflow: WorkflowBuilder<Shop, Async>,
        step: StepBuilder<Async>,
    ) = workflow.step(step.build())
}

/** Keeps the kind of every event it receives, in order. */
class AsyncLog : AsyncReporter {
    val kinds = mutableListOf<String>()

    override suspend fun report(event: Event) {
        kinds += event.kind.toString()
    }
}
