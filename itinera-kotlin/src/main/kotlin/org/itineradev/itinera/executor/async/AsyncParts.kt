package org.itineradev.itinera.executor.async

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.itineradev.itinera.engine.Delivery
import org.itineradev.itinera.engine.Emits
import org.itineradev.itinera.engine.Guard
import org.itineradev.itinera.engine.Handle
import org.itineradev.itinera.engine.HookCaller
import org.itineradev.itinera.engine.Parts
import org.itineradev.itinera.engine.ReporterHandle
import org.itineradev.itinera.engine.ReporterHandles
import org.itineradev.itinera.engine.RescueRunning
import org.itineradev.itinera.engine.Running
import org.itineradev.itinera.engine.StepCaller
import org.itineradev.itinera.engine.nullable
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.policy.async.AsyncHook
import org.itineradev.itinera.policy.async.AsyncWorkflowHook
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.report.async.AsyncDispatcher
import org.itineradev.itinera.report.async.AsyncDispatcherFactory
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.async.AsyncProvider
import org.itineradev.itinera.step.RescueFactory
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.async.AsyncRescueFactory
import org.itineradev.itinera.step.async.AsyncRescueRun
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.async.AsyncInputAdapter

/** The asynchronous mode's parts, whose dispatchers [factory] makes. */
internal class AsyncParts(private val factory: AsyncDispatcherFactory) : Parts<Async> {
    override val mode: Async
        get() = Async

    override fun accepts(factory: StepFactory<*>?): Boolean = factory is AsyncStepFactory

    override fun accepts(rescue: RescueFactory<*>?): Boolean = rescue is AsyncRescueFactory

    override fun accepts(hook: HookCall<*, *, *, *>?): Boolean =
        (hook is AsyncHook<*, *, *> || hook is AsyncWorkflowHook<*, *>) && writtenInKotlin(hook)

    override fun accepts(adapter: InputAdapterCall<*, *>?): Boolean =
        adapter is AsyncInputAdapter && writtenInKotlin(adapter)

    override fun accepts(provider: ProviderCall<*, *, *, *>?): Boolean =
        provider is AsyncProvider<*, *, *> && writtenInKotlin(provider)

    override fun accepts(reporter: ReporterOf<*>?): Boolean =
        reporter is AsyncReporter && writtenInKotlin(reporter)

    @JvmSynthetic
    override suspend fun dispatcher(): Delivery? = nullable(factory.create())?.let(::AsyncDelivery)

    @JvmSynthetic
    override suspend fun adapt(
        adapter: InputAdapterCall<*, Async>,
        workflow: Any?,
        got: Got<InputAdapterKind>,
    ): Supplied? = nullable(returned<AsyncInputAdapter<Any?>>(adapter).adapt(workflow, got))

    @JvmSynthetic
    override suspend fun provide(
        provider: ProviderCall<*, *, *, *>?,
        workflow: Any?,
        got: Got<*>,
    ): Provided<*>? =
        nullable(
            returned<AsyncProvider<Any?, Any?, Any>>(provider).provide(workflow, returned(got))
        )

    override val steps: StepCaller<Async>
        get() = AsyncSteps

    override val reporterHandles: ReporterHandles
        get() = AsyncReporterHandles

    override val hooks: HookCaller
        get() = AsyncHooks

    @JvmSynthetic
    override suspend fun ensureActive() {
        currentCoroutineContext().ensureActive()
    }
}

/**
 * Whether [part] was written in Kotlin, or is a lambda: javac lets a Java class implement an
 * asynchronous interface without writing its `suspend` operations, which it cannot see, so such a
 * class is a part of the other mode.
 */
internal fun writtenInKotlin(part: Any): Boolean =
    part.javaClass.isHidden || part.javaClass.isAnnotationPresent(Metadata::class.java)

private fun running(run: AsyncStepRun): Running = Running { run.run() }

private fun rescuing(run: AsyncRescueRun): RescueRunning = RescueRunning { run.run() }

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

/** Makes the asynchronous mode's reporter handles. */
private object AsyncReporterHandles : ReporterHandles {
    override fun stepReporter(emits: Emits): ReporterHandle =
        EmittingAsyncReporter(emits, Handle.ASYNC_STEP_REPORTER)

    override fun hookReporter(emits: Emits): ReporterHandle =
        EmittingAsyncReporter(emits, Handle.ASYNC_HOOK_REPORTER)

    override fun providerReporter(emits: Emits): ReporterHandle =
        EmittingAsyncReporter(emits, Handle.ASYNC_PROVIDER_REPORTER)
}

/** Builds the asynchronous mode's steps and rescue steps, awaiting each. */
private object AsyncSteps : StepCaller<Async> {
    @JvmSynthetic
    override suspend fun build(factory: StepFactory<Async>, got: Got<StepKind<Async>>): Running? =
        nullable((factory as AsyncStepFactory).build(got))?.let(::running)

    @JvmSynthetic
    override suspend fun rescue(
        factory: RescueFactory<Async>,
        got: Got<RescueKind<Async>>,
    ): RescueRunning? = nullable((factory as AsyncRescueFactory).build(got))?.let(::rescuing)
}

/** Calls the asynchronous mode's hooks, awaiting each. */
private object AsyncHooks : HookCaller {
    @JvmSynthetic
    override suspend fun step(hook: HookCall<*, *, *, *>, policy: Any?, got: Got<*>): Any? =
        returned<AsyncHook<Any?, Any?, Any?>>(hook).call(policy, returned(got))

    @JvmSynthetic
    override suspend fun workflow(hook: HookCall<*, *, *, *>, policy: Any?, got: Got<*>) {
        returned<AsyncWorkflowHook<Any?, Any?>>(hook).call(policy, returned(got))
    }
}
