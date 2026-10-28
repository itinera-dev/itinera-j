package org.itineradev.itinera.executor.async

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.itineradev.itinera.engine.Delivery
import org.itineradev.itinera.engine.Guard
import org.itineradev.itinera.engine.Parts
import org.itineradev.itinera.engine.Running
import org.itineradev.itinera.engine.nullable
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.report.async.AsyncDispatcher
import org.itineradev.itinera.report.async.AsyncDispatcherFactory
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async

/** The asynchronous mode's parts, whose dispatchers [factory] makes. */
internal class AsyncParts(private val factory: AsyncDispatcherFactory) : Parts<Async> {
    override val mode: Async
        get() = Async

    override fun accepts(factory: StepFactory<*>?): Boolean = factory is AsyncStepFactory

    override fun accepts(reporter: ReporterOf<*>?): Boolean = reporter is AsyncReporter

    @JvmSynthetic
    override suspend fun dispatcher(): Delivery? = nullable(factory.create())?.let(::AsyncDelivery)

    @JvmSynthetic
    override suspend fun build(factory: StepFactory<Async>, got: Got<StepKind<Async>>): Running? =
        nullable((factory as AsyncStepFactory).build(got))?.let(::running)

    @JvmSynthetic
    override suspend fun ensureActive() {
        currentCoroutineContext().ensureActive()
    }
}

private fun running(run: AsyncStepRun): Running = Running { run.run() }

private class AsyncDelivery(private val dispatcher: AsyncDispatcher) : Delivery {
    @JvmSynthetic
    override suspend fun add(reporter: ReporterOf<*>, guard: Guard) {
        dispatcher.add(Guarded(reporter as AsyncReporter, guard))
    }

    @JvmSynthetic
    override suspend fun dispatch(event: Event) {
        dispatcher.dispatch(event)
    }

    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        dispatcher.setGlobalMetadata(key, value)
    }
}

private class Guarded(private val reporter: AsyncReporter, private val guard: Guard) :
    AsyncReporter {
    @JvmSynthetic
    override suspend fun report(event: Event) {
        guard.report(event.kind) { reporter.report(event) }
    }

    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        guard.setGlobalMetadata { reporter.setGlobalMetadata(key, value) }
    }
}
