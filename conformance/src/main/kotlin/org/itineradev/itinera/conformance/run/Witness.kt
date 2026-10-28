package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepRun

/** Every step the scenario's workflow built, by step name, in the order built, and how many ran. */
internal class Witness {
    private val built = linkedMapOf<String, MutableList<Any>>()

    /** How many built steps ran. */
    var ran = 0
        private set

    /** The steps built under [step], in the order they were built. */
    fun built(step: String): List<Any> = built[step].orEmpty().toList()

    /**
     * A factory building with [factory], each step kept under [step] as it is built and counted as
     * it runs.
     */
    fun watching(step: String, factory: SyncStepFactory): SyncStepFactory = SyncStepFactory { got ->
        watched(step, factory.build(got))
    }

    /** The asynchronous twin of [watching]. */
    fun watchingAsync(step: String, factory: AsyncStepFactory): AsyncStepFactory =
        AsyncStepFactory { got ->
            watchedAsync(step, factory.build(got))
        }

    private fun watched(step: String, run: SyncStepRun): SyncStepRun {
        keep(step, run)
        return SyncStepRun {
            ran += 1
            run.run()
        }
    }

    private fun watchedAsync(step: String, run: AsyncStepRun): AsyncStepRun {
        keep(step, run)
        return AsyncStepRun {
            ran += 1
            run.run()
        }
    }

    private fun keep(step: String, run: Any) {
        built.getOrPut(step, ::mutableListOf) += run
    }
}
