package org.itineradev.itinera.workflow.async

import java.lang.reflect.Method
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Builders
import org.itineradev.itinera.engine.Described
import org.itineradev.itinera.engine.StepClass
import org.itineradev.itinera.engine.StepFactories
import org.itineradev.itinera.engine.invoked
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.request.async.asyncStepReporter
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
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
            Described.read<Any?, Async>(type, Async, AsynchronousSteps)
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

/**
 * Calls the `suspend` method [method] of [target] with [arguments] and awaits what it returns,
 * throwing what the method itself threw.
 */
@JvmSynthetic
private suspend fun suspended(method: Method, target: Any, arguments: Array<Any?>): Any? =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        invoked(method, target, *arguments, continuation)
    }
