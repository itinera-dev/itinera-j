package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.workflow.Async

/** A scripted run of an asynchronous workflow, as [written] so far. */
internal class AsyncWrittenScript(private val written: Written<Async>) : AsyncScript {
    override fun step(name: String): NamedStep<AsyncStepScript> =
        AsyncStepWriter(this, written.step(name))

    override fun ensureAllScripted(): AsyncScript = apply { written.ensureAllScripted() }

    @JvmSynthetic
    override suspend fun run(executor: AsyncLocalExecutor): ScriptedRun {
        val (playing, instance) = written.playing()
        return playing.ran(executor.run(instance))
    }
}

/** A scripted run of an asynchronous workflow describing the attempts of [target], in [script]. */
private class AsyncStepWriter(private val script: AsyncWrittenScript, target: Target<*>) :
    StepWriter<AsyncStepScript>(target), AsyncStepScript {
    override val next: AsyncStepScript
        get() = this

    override fun andStep(name: String): NamedStep<AsyncStepScript> = script.step(name)

    override fun ensureAllScripted(): AsyncStepScript = apply { script.ensureAllScripted() }

    @JvmSynthetic
    override suspend fun run(executor: AsyncLocalExecutor): ScriptedRun = script.run(executor)
}
