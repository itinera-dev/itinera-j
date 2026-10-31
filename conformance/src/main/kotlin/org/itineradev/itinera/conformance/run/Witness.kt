package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.ProviderRequest
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepRun

/** One step built for an attempt: the step itself, and what it received. */
internal data class Built(val step: Any, val received: Received)

/**
 * Every step the scenario's workflow built, by step name, in the order built, how many ran, and
 * what each service provider received on each call.
 */
internal class Witness {
    private val built = linkedMapOf<String, MutableList<Built>>()
    private val calls = linkedMapOf<String, MutableList<Map<ProviderRequest, Any?>>>()

    /** How many built steps ran. */
    var ran = 0
        private set

    /** The steps built under [step], in the order they were built. */
    fun built(step: String): List<Built> = built[step].orEmpty().toList()

    /** What the provider [provider] received on each call, in order. */
    fun calls(provider: String): List<Map<ProviderRequest, Any?>> =
        calls[provider].orEmpty().toList()

    /** Records a call of the provider [provider], which received [received]. */
    fun called(provider: String, received: Map<ProviderRequest, Any?>) {
        calls.getOrPut(provider, ::mutableListOf) += received
    }

    /**
     * A factory building with [factory], each step kept under [step] with what it received as it is
     * built, and counted as it runs, where it first acts as [scripted] says.
     */
    fun watching(step: String, factory: SyncStepFactory, scripted: Scripted): SyncStepFactory =
        SyncStepFactory { got ->
            val received = scripted.receivedFrom(got)
            val run = factory.build(got)
            keep(step, run, received)
            SyncStepRun {
                ran += 1
                scripted.act(received)
                run.run()
            }
        }

    /** The asynchronous twin of [watching]. */
    fun watchingAsync(
        step: String,
        factory: AsyncStepFactory,
        scripted: Scripted,
    ): AsyncStepFactory = AsyncStepFactory { got ->
        val received = scripted.receivedFrom(got)
        val run = factory.build(got)
        keep(step, run, received)
        AsyncStepRun {
            ran += 1
            scripted.act(received)
            run.run()
        }
    }

    private fun keep(step: String, run: Any, received: Received) {
        built.getOrPut(step, ::mutableListOf) += Built(run, received)
    }
}
