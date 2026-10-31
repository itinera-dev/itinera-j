package org.itineradev.itinera.workflow.async

import java.lang.reflect.Method
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.AdapterMethod
import org.itineradev.itinera.engine.Builders
import org.itineradev.itinera.engine.Described
import org.itineradev.itinera.engine.HookCalls
import org.itineradev.itinera.engine.HookMethod
import org.itineradev.itinera.engine.ProviderCalls
import org.itineradev.itinera.engine.ProviderMethod
import org.itineradev.itinera.engine.StepClass
import org.itineradev.itinera.engine.StepFactories
import org.itineradev.itinera.engine.invoked
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.async.AsyncHook
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.policy.async.AsyncWorkflowHook
import org.itineradev.itinera.request.HookReporterKind
import org.itineradev.itinera.request.ProviderReporterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.request.async.asyncHookReporter
import org.itineradev.itinera.request.async.asyncProviderReporter
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.ProviderReporter
import org.itineradev.itinera.service.async.AsyncProvider
import org.itineradev.itinera.service.async.AsyncProviderReporter
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * Starts the descriptor of an asynchronous workflow named [name], whose instances are created with
 * objects of [W].
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncBuilding
 */
@ExperimentalItineraApi
public fun <W> asyncBuilder(name: String): WorkflowBuilder<W, Async> =
    Builders.workflow(name, Async)

/**
 * Starts the descriptor of an asynchronous step, which the factory builds for each attempt, with a
 * retry budget of 0 and abnormal terminations retried, unless the builder says otherwise.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncBuilding
 */
@ExperimentalItineraApi
public fun asyncStep(name: String, factory: AsyncStepFactory): StepBuilder<Async> =
    Builders.step(name, factory)

/**
 * Starts the descriptor of an asynchronous input adapter named [name], attached to the step named
 * [step], to which the builder may attach others.
 *
 * @throws IllegalArgumentException when [name] or [step] is empty.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncAdapters
 */
@ExperimentalItineraApi
public fun <W> asyncInputAdapter(
    name: String,
    step: String,
    adapter: AsyncInputAdapter<W>,
): InputAdapterBuilder<W, Async> = Builders.inputAdapter(name, adapter).step(step)

/**
 * The descriptor of an asynchronous workflow written as an annotated class, read once and then
 * shared by every caller.
 *
 * @throws Violations with every mistake in the class's declaration.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncDescribing
 */
@ExperimentalItineraApi
public fun <W : ItineraAsyncWorkflow> asyncDescriptorOf(
    type: KClass<W>
): WorkflowDescriptor<W, Async> = returned(asynchronous.get(type.java))

private val asynchronous =
    object : ClassValue<WorkflowDescriptor<*, Async>>() {
        override fun computeValue(type: Class<*>): WorkflowDescriptor<*, Async> =
            Described.read<Any?, Async>(
                type,
                Async,
                AsynchronousSteps,
                AsynchronousHooks,
                ::asynchronous,
                AsynchronousProviders,
            )
    }

private object AsynchronousSteps : StepFactories<Async> {
    override val handles: Map<Class<*>, Request<StepKind<Async>, *>> =
        mapOf(AsyncStepReporter::class.java to Requests.asyncStepReporter())

    override val otherModes: Map<String, String> =
        mapOf(
            StepReporter::class.java.name to
                "requests StepReporter, which only a synchronous step can"
        )

    override fun of(step: StepClass<Async>): StepFactory<Async> = AsyncStepFactory { got ->
        val built = step.built(got)
        val arguments = step.arguments(got)
        AsyncStepRun { returned(suspended(step.run, built, arguments)) }
    }
}

private object AsynchronousHooks : HookCalls<Async> {
    override val handles: Map<Class<*>, Request<HookReporterKind<Async>, *>> =
        mapOf(AsyncHookReporter::class.java to Requests.asyncHookReporter())

    override val otherModes: Map<String, String> =
        mapOf(
            HookReporter::class.java.name to
                "requests HookReporter, which only a synchronous hook can"
        )

    override fun <K, R> stepHook(hook: HookMethod): HookCall<Async, Any, K, R> =
        AsyncHook { policy, got ->
            returned(suspended(hook.method, policy, hook.arguments(got)))
        }

    override fun <K> workflowHook(hook: HookMethod): HookCall<Async, Any, K, Unit> =
        AsyncWorkflowHook { policy, got ->
            suspended(hook.method, policy, hook.arguments(got))
        }
}

private object AsynchronousProviders : ProviderCalls<Async> {
    override val handles: Map<Class<*>, Request<ProviderReporterKind<Async>, *>> =
        mapOf(AsyncProviderReporter::class.java to Requests.asyncProviderReporter())

    override val otherModes: Map<String, String> =
        mapOf(
            ProviderReporter::class.java.name to
                "requests ProviderReporter, which only a synchronous provider can"
        )

    override fun <K> of(provider: ProviderMethod<K>): ProviderCall<Async, Any?, K, Any> =
        AsyncProvider { workflow, got ->
            returned(suspended(provider.method, workflow, provider.arguments(got)))
        }
}

private fun asynchronous(adapter: AdapterMethod): InputAdapterCall<Any?, Async> =
    AsyncInputAdapter { workflow, got ->
        returned(suspended(adapter.method, workflow, adapter.arguments(got)))
    }

/**
 * Calls the `suspend` method [method] of [target] with [arguments] and awaits what it returns,
 * throwing what the method itself threw.
 */
@JvmSynthetic
private suspend fun suspended(method: Method, target: Any?, arguments: Array<Any?>): Any? =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        invoked(method, target, *arguments, continuation)
    }
