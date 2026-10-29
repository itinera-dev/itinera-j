package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.workflow.Sync

/** A scripted run of a synchronous workflow, as [written] so far. */
internal class WrittenScript(private val written: Written<Sync>) : Script {
    override fun step(name: String): NamedStep<StepScript> =
        SyncStepWriter(this, written.step(name))

    override fun ensureAllScripted(): Script = apply { written.ensureAllScripted() }

    override fun run(): ScriptedRun = run(LocalExecutor())

    override fun run(executor: LocalExecutor): ScriptedRun {
        val (playing, instance) = written.playing()
        return playing.ran(executor.run(instance))
    }
}

/** A scripted run of a synchronous workflow describing the attempts of [target], in [script]. */
private class SyncStepWriter(private val script: WrittenScript, target: Target<*>) :
    StepWriter<StepScript>(target), StepScript {
    override val next: StepScript
        get() = this

    override fun andStep(name: String): NamedStep<StepScript> = script.step(name)

    override fun ensureAllScripted(): StepScript = apply { script.ensureAllScripted() }

    override fun run(): ScriptedRun = script.run()

    override fun run(executor: LocalExecutor): ScriptedRun = script.run(executor)
}
