package org.itineradev.itinera.engine

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueFactory
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Supplied

/**
 * What the engine calls in one mode: which parts it accepts, and how it makes the dispatcher, calls
 * input adapters and service providers, builds a step, makes the reporter handles and asks whether
 * its journey was cancelled. [SyncParts] serves the synchronous mode, and `itinera-kotlin` the
 * asynchronous one.
 *
 * Part of itinera's engine, not of its API.
 */
public interface Parts<M : Mode> {
    /** The mode it serves. */
    public val mode: M

    /** Whether [factory] builds a step of this mode. */
    public fun accepts(factory: StepFactory<*>?): Boolean

    /** Whether [rescue] builds a rescue step of this mode. */
    public fun accepts(rescue: RescueFactory<*>?): Boolean

    /** Whether [hook] is a hook of this mode. */
    public fun accepts(hook: HookCall<*, *, *, *>?): Boolean

    /** Whether [adapter] is an input adapter of this mode. */
    public fun accepts(adapter: InputAdapterCall<*, *>?): Boolean

    /** Whether [provider] is a service provider of this mode. */
    public fun accepts(provider: ProviderCall<*, *, *, *>?): Boolean

    /** Whether [reporter] is a reporter of this mode. */
    public fun accepts(reporter: ReporterOf<*>?): Boolean

    /**
     * Makes the journey's dispatcher with the executor's dispatcher factory: `null` when the
     * factory returned `null`, which only Java code can. What the factory throws is thrown.
     */
    @JvmSynthetic public suspend fun dispatcher(): Delivery?

    /**
     * Calls [adapter], which [accepts] took, with [workflow] and what it [got]: `null` when it
     * returned `null`, which only Java code can. What it throws is thrown.
     */
    @JvmSynthetic
    public suspend fun adapt(
        adapter: InputAdapterCall<*, M>,
        workflow: Any?,
        got: Got<InputAdapterKind>,
    ): Supplied?

    /**
     * Calls [provider], which [accepts] took, with [workflow] and what it [got]: `null` when it
     * returned `null`, which only Java code can. What it throws is thrown.
     */
    @JvmSynthetic
    public suspend fun provide(
        provider: ProviderCall<*, *, *, *>?,
        workflow: Any?,
        got: Got<*>,
    ): Provided<*>?

    /**
     * Builds the step for one attempt with [factory], from what it [got]: `null` when the factory
     * returned `null`, which only Java code can. What the factory throws is thrown.
     */
    @JvmSynthetic public suspend fun build(factory: StepFactory<M>, got: Got<StepKind<M>>): Running?

    /** What makes the reporter handles of this mode. */
    public val reporterHandles: ReporterHandles

    /**
     * Throws a `CancellationException` when the coroutine running the journey was cancelled, and
     * does nothing otherwise, or in a mode that cannot be cancelled.
     */
    @JvmSynthetic public suspend fun ensureActive()
}

/**
 * The journey's dispatcher, as the engine calls it in either mode.
 *
 * Part of itinera's engine, not of its API.
 */
public interface Delivery {
    /** Adds [reporter], which [accepts][Parts.accepts] took, through [guard]. */
    @JvmSynthetic public suspend fun add(reporter: ReporterOf<*>, guard: Guard)

    /** Dispatches [event] to every reporter. */
    @JvmSynthetic public suspend fun dispatch(event: Event)

    /** Sets global metadata on every reporter. */
    @JvmSynthetic public suspend fun setGlobalMetadata(key: String, value: Any?)
}

/**
 * One attempt's run of a built step, as the engine calls it in either mode.
 *
 * Part of itinera's engine, not of its API.
 */
public fun interface Running {
    /** Runs the attempt: `null` when the step returned `null`, which only Java code can. */
    @JvmSynthetic public suspend fun run(): Outcome?
}

/**
 * Calls custom code and gives what it returned or threw. An [Error] is never caught, and when the
 * code threw, the journey is abandoned if its coroutine was cancelled meanwhile.
 */
@JvmSynthetic
internal suspend inline fun <T> Parts<*>.calling(call: () -> T): Result<T> {
    val result = runCatching(call).onFailure(::rethrowError)
    if (result.isFailure) ensureActive()
    return result
}
