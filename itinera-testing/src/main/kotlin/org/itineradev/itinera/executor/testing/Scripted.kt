@file:JvmName("Scripted")

package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Sync

/**
 * Starts a scripted run of [instance], a synchronous workflow's: a script that says, for the steps
 * it names, what their attempts do in place of running the steps. Every step is still built for
 * real, its inputs, services and constructor included; only the call to its run is replaced, and a
 * step the script does not name runs for real.
 *
 * A mistake in writing the script throws a [TestMistake] where it is written, and what the script
 * leaves out makes `run` throw one, before the journey starts.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
public fun scripted(instance: WorkflowInstance<Sync>): Script = WrittenScript(Written(instance))

/**
 * Starts a scripted run of [instance], an asynchronous workflow's, as `scripted` does for a
 * synchronous one.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
 */
@JvmName("scriptedAsync")
@JvmSynthetic
public fun scripted(instance: WorkflowInstance<Async>): AsyncScript =
    AsyncWrittenScript(Written(instance))
