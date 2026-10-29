package org.itineradev.itinera.engine

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.report.Dispatcher
import org.itineradev.itinera.report.DispatcherFactory
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.SyncProvider
import org.itineradev.itinera.step.RescueFactory
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.SyncRescueFactory
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.SyncInputAdapter

/**
 * The synchronous mode's parts, whose dispatchers [factory] makes.
 *
 * Part of itinera's engine, not of its API.
 */
public class SyncParts(private val factory: DispatcherFactory) : Parts<Sync> {
    override val mode: Sync
        get() = Sync

    override fun accepts(factory: StepFactory<*>?): Boolean = factory is SyncStepFactory

    override fun accepts(rescue: RescueFactory<*>?): Boolean = rescue is SyncRescueFactory

    override fun accepts(hook: HookCall<*, *, *, *>?): Boolean =
        hook is SyncHook<*, *, *> || hook is SyncWorkflowHook<*, *>

    override fun accepts(adapter: InputAdapterCall<*, *>?): Boolean = adapter is SyncInputAdapter

    override fun accepts(provider: ProviderCall<*, *, *, *>?): Boolean =
        provider is SyncProvider<*, *, *>

    override fun accepts(reporter: ReporterOf<*>?): Boolean = reporter is Reporter

    @JvmSynthetic
    override suspend fun dispatcher(): Delivery? = nullable(factory.create())?.let(::SyncDelivery)

    @JvmSynthetic
    override suspend fun adapt(
        adapter: InputAdapterCall<*, Sync>,
        workflow: Any?,
        got: Got<InputAdapterKind>,
    ): Supplied? = nullable(returned<SyncInputAdapter<Any?>>(adapter).adapt(workflow, got))

    @JvmSynthetic
    override suspend fun provide(
        provider: ProviderCall<*, *, *, *>?,
        workflow: Any?,
        got: Got<*>,
    ): Provided<*>? =
        nullable(returned<SyncProvider<Any?, Any?, Any>>(provider).provide(workflow, returned(got)))

    @JvmSynthetic
    override suspend fun build(factory: StepFactory<Sync>, got: Got<StepKind<Sync>>): Running? =
        nullable((factory as SyncStepFactory).build(got))?.let { run -> Running(run::run) }

    override val reporterHandles: ReporterHandles
        get() = SyncReporterHandles

    override val hooks: HookCaller
        get() = SyncHooks

    @JvmSynthetic override suspend fun ensureActive(): Unit = Unit
}

private class SyncDelivery(private val dispatcher: Dispatcher) : Delivery {
    @JvmSynthetic
    override suspend fun add(reporter: ReporterOf<*>, guard: Guard) {
        dispatcher.add(Guarded(reporter as Reporter, guard))
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

private class Guarded(private val reporter: Reporter, private val guard: Guard) : Reporter {
    override fun report(event: Event) {
        guard.report(event.kind) { reporter.report(event) }
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        guard.setGlobalMetadata { reporter.setGlobalMetadata(key, value) }
    }
}

/** Makes the synchronous mode's reporter handles. */
private object SyncReporterHandles : ReporterHandles {
    override fun stepReporter(emits: Emits): ReporterHandle =
        EmittingReporter(emits, Handle.STEP_REPORTER)

    override fun hookReporter(emits: Emits): ReporterHandle =
        EmittingReporter(emits, Handle.HOOK_REPORTER)

    override fun providerReporter(emits: Emits): ReporterHandle =
        EmittingReporter(emits, Handle.PROVIDER_REPORTER)
}

/** Calls the synchronous mode's hooks. */
private object SyncHooks : HookCaller {
    @JvmSynthetic
    override suspend fun step(hook: HookCall<*, *, *, *>, policy: Any?, got: Got<*>): Any? =
        returned<SyncHook<Any?, Any?, Any?>>(hook).call(policy, returned(got))

    @JvmSynthetic
    override suspend fun workflow(hook: HookCall<*, *, *, *>, policy: Any?, got: Got<*>) {
        returned<SyncWorkflowHook<Any?, Any?>>(hook).call(policy, returned(got))
    }
}
