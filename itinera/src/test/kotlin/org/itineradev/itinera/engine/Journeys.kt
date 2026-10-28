package org.itineradev.itinera.engine

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.DefaultDispatcherFactory
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** Workflows, reporters and journeys for the tests of the engine and the executor. */
@OptIn(ExperimentalItineraApi::class)
object Journeys {
    /** The workflow object of every workflow the tests build. */
    class Shop

    val clock: Clock = Clock.fixed(Instant.parse("2026-10-28T09:00:00Z"), ZoneOffset.UTC)

    fun succeeding(name: String): StepBuilder<Sync> =
        StepDescriptor.builder(name) { SyncStepRun { Outcome.success() } }

    /** A step whose attempts play [endings] in turn: `null` succeeds, anything else is thrown. */
    fun playing(name: String, vararg endings: Throwable?): StepBuilder<Sync> {
        val left = endings.toMutableList()
        return StepDescriptor.builder(name) { SyncStepRun { ending(left.removeFirst()) } }
    }

    /** A step that requests its contributor, whose attempts play [attempts] in turn with it. */
    fun contributing(name: String, vararg attempts: (Contributor) -> Outcome): StepBuilder<Sync> {
        val left = attempts.toMutableList()
        val contributor = Requests.contributor()
        return StepDescriptor.builder(name) { got ->
                val handle = got[contributor]
                val attempt = left.removeFirst()
                SyncStepRun { attempt(handle) }
            }
            .request(contributor)
    }

    /** A step that requests its step reporter, whose attempts play [attempts] in turn with it. */
    fun reporting(name: String, vararg attempts: (StepReporter) -> Outcome): StepBuilder<Sync> {
        val left = attempts.toMutableList()
        val reporter = Requests.stepReporter()
        return StepDescriptor.builder(name) { got ->
                val handle = got[reporter]
                val attempt = left.removeFirst()
                SyncStepRun { attempt(handle) }
            }
            .request(reporter)
    }

    fun building(name: String, factory: SyncStepFactory): StepBuilder<Sync> =
        StepDescriptor.builder(name, factory)

    fun shop(): WorkflowBuilder<Shop, Sync> = WorkflowDescriptor.builder(Shop::class.java, "shop")

    fun instanceOf(descriptor: WorkflowDescriptor<Shop, Sync>): InstanceBuilder =
        descriptor.instance(Shop())

    /** The instance of a workflow of [steps], in order. */
    fun journeyOf(vararg steps: StepBuilder<Sync>): InstanceBuilder =
        instanceOf(steps.fold(shop(), ::withStep).build())

    /** The instance of a workflow of one step, `pack`, that succeeds. */
    fun packing(): InstanceBuilder = journeyOf(succeeding("pack"))

    private fun withStep(workflow: WorkflowBuilder<Shop, Sync>, step: StepBuilder<Sync>) =
        workflow.step(step.build())

    /** Runs [instance] with the fixed clock, as `LocalExecutor` does with the system's. */
    fun ran(
        instance: WorkflowInstance<Sync>,
        factory: DispatcherFactory = DefaultDispatcherFactory(),
    ): JourneyResult = driven { Engine(SyncParts(factory), clock).run(instance) }

    private fun ending(thrown: Throwable?): Outcome =
        if (thrown == null) Outcome.success() else throw thrown
}

/** Keeps every event and entry of global metadata it receives, in order. */
@OptIn(ExperimentalItineraApi::class)
class Log : Reporter {
    val events = mutableListOf<Event>()
    val metadata = mutableListOf<Pair<String, Any?>>()

    val kinds: List<String>
        get() = events.map(::kindOf)

    override fun report(event: Event) {
        events += event
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        metadata += key to value
    }
}

@OptIn(ExperimentalItineraApi::class)
private fun kindOf(event: Event): String = event.kind.toString()
