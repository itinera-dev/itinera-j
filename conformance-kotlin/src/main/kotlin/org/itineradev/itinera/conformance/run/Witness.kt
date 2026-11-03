package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.ProviderRequest
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.SyncRescueFactory
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.step.async.AsyncRescueFactory
import org.itineradev.itinera.step.async.AsyncRescueRun
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Sync

/** One step or rescue step built for an attempt: the step itself, and what it received. */
internal data class Built(val step: Any, val received: Received<*>)

/**
 * Every step and rescue step the scenario's workflow built, by name, in the order built, how many
 * ran, and what each service provider received on each call.
 */
internal class Witness {
    private val built = linkedMapOf<String, MutableList<Built>>()
    private val calls = linkedMapOf<String, MutableList<Map<ProviderRequest, Any?>>>()
    private val hookCalls = mutableListOf<HookCall>()
    private val roleCalls = mutableListOf<Pair<String, String>>()

    /** How many built steps ran. */
    var ran = 0
        private set

    /** The steps built under [step], in the order they were built. */
    fun built(step: String): List<Built> = built[step].orEmpty().toList()

    /** What the provider [provider] received on each call, in order. */
    fun calls(provider: String): List<Map<ProviderRequest, Any?>> =
        calls[provider].orEmpty().toList()

    /** Every call of a hook, in the order made. */
    val hooksCalled: List<HookCall>
        get() = hookCalls.toList()

    /** Records a call of a hook. */
    fun hookCalled(call: HookCall) {
        hookCalls += call
    }

    /** How many times [operation] of the role [role] was called. */
    fun roleCalls(role: String, operation: String): Int =
        roleCalls.count((role to operation)::equals)

    /** Records a call of [operation] of the role [role]. */
    fun roleCalled(role: String, operation: String) {
        roleCalls += role to operation
    }

    /** Records a call of the provider [provider], which received [received]. */
    fun called(provider: String, received: Map<ProviderRequest, Any?>) {
        calls.getOrPut(provider, ::mutableListOf) += received
    }

    /**
     * A factory building with [factory], each step kept under [step] with what it received as it is
     * built, and counted as it runs, where it first acts as [scripted] says.
     */
    fun watching(
        step: String,
        factory: SyncStepFactory,
        scripted: Scripted<Sync, StepReporter>,
    ): SyncStepFactory = SyncStepFactory { got ->
        val received = scripted.receivedFrom(got)
        val run = factory.build(got)
        keep(step, run, received)
        SyncStepRun {
            ran += 1
            scripted.prepared(received).forEach { action ->
                perform(action, received, scripted.ignoresEmitFailures)
            }
            run.run()
        }
    }

    /** The asynchronous twin of [watching]. */
    fun watchingAsync(
        step: String,
        factory: AsyncStepFactory,
        scripted: Scripted<Async, AsyncStepReporter>,
    ): AsyncStepFactory = AsyncStepFactory { got ->
        val received = scripted.receivedFrom(got)
        val run = factory.build(got)
        keep(step, run, received)
        AsyncStepRun {
            ran += 1
            scripted.prepared(received).forEach { action ->
                performAsync(action, received, scripted.ignoresEmitFailures)
            }
            run.run()
        }
    }

    /**
     * A factory building rescue steps with [factory], each kept under [step] with what it received
     * as it is built, and counted as it runs, where it first acts as [scripted] says.
     */
    fun watchingRescue(
        step: String,
        factory: SyncRescueFactory,
        scripted: Scripted<Sync, StepReporter>,
    ): SyncRescueFactory = SyncRescueFactory { got ->
        val received = scripted.rescueReceivedFrom(got)
        val run = factory.build(got)
        keep(step, run, received)
        SyncRescueRun {
            ran += 1
            scripted.prepared(received).forEach { action ->
                perform(action, received, scripted.ignoresEmitFailures)
            }
            run.run()
        }
    }

    /** The asynchronous twin of [watchingRescue]. */
    fun watchingAsyncRescue(
        step: String,
        factory: AsyncRescueFactory,
        scripted: Scripted<Async, AsyncStepReporter>,
    ): AsyncRescueFactory = AsyncRescueFactory { got ->
        val received = scripted.rescueReceivedFrom(got)
        val run = factory.build(got)
        keep(step, run, received)
        AsyncRescueRun {
            ran += 1
            scripted.prepared(received).forEach { action ->
                performAsync(action, received, scripted.ignoresEmitFailures)
            }
            run.run()
        }
    }

    private fun keep(step: String, run: Any, received: Received<*>) {
        built.getOrPut(step, ::mutableListOf) += Built(run, received)
    }
}
