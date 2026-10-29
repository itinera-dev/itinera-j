package org.itineradev.itinera.conformance.run

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.TestSupport
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.executor.testing.AsyncStepScript
import org.itineradev.itinera.executor.testing.ScriptedRun
import org.itineradev.itinera.executor.testing.StepScript
import org.itineradev.itinera.executor.testing.scripted
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Sync

/**
 * Runs [instance] with the script [testing] holds, written through test support, on a
 * `LocalExecutor` given no dispatcher factory.
 *
 * @throws org.itineradev.itinera.testing.TestMistake when the script is mistaken.
 */
internal fun scriptedRunOf(instance: WorkflowInstance<Sync>, testing: TestSupport): ScriptedRun {
    val script = scripted(instance)
    if (testing.ensuresAllScripted) script.ensureAllScripted()
    val written =
        ScriptWriting<StepScript>(script::step, StepScript::andStep).written(rowsOf(testing))
    return written.run(LocalExecutor())
}

/** The asynchronous twin of [scriptedRunOf], on an `AsyncLocalExecutor`. */
@JvmName("asyncScriptedRunOf")
internal fun scriptedRunOf(instance: WorkflowInstance<Async>, testing: TestSupport): ScriptedRun {
    val script = scripted(instance)
    if (testing.ensuresAllScripted) script.ensureAllScripted()
    val written =
        ScriptWriting<AsyncStepScript>(script::step, AsyncStepScript::andStep)
            .written(rowsOf(testing))
    return runBlocking { written.run(AsyncLocalExecutor()) }
}

private fun rowsOf(testing: TestSupport) =
    testing.script ?: throw ModelException("no script is written")
