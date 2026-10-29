package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.testing.TestMistake

/**
 * A scripted run of an asynchronous workflow before it names a step, as [Script] is for a
 * synchronous one.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
 */
public interface AsyncScript {
    /**
     * Starts describing the step, or the rescue step, named [name].
     *
     * @throws TestMistake when the workflow has no step or rescue step named [name], or the script
     *   named it before.
     * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
     */
    public fun step(name: String): NamedStep<AsyncStepScript>

    /**
     * Asks that every step of the workflow be scripted, which [run] checks.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
     */
    public fun ensureAllScripted(): AsyncScript

    /**
     * Runs the journey on [executor], a new [AsyncLocalExecutor] unless one is given, playing the
     * script, and returns the scripted run, as [Script.run] does.
     *
     * @throws TestMistake as [Script.run] says.
     * @throws Refusal as [Script.run] says.
     * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
     */
    @JvmSynthetic
    public suspend fun run(executor: AsyncLocalExecutor = AsyncLocalExecutor()): ScriptedRun
}

/**
 * A scripted run of an asynchronous workflow once a step has described an attempt, as [StepScript]
 * is for a synchronous one.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
 */
public interface AsyncStepScript : AttemptChoice<AsyncStepScript> {
    /**
     * Starts describing the next step, or rescue step, named [name].
     *
     * @throws TestMistake when the workflow has no step or rescue step named [name], or the script
     *   named it before.
     * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
     */
    public fun andStep(name: String): NamedStep<AsyncStepScript>

    /**
     * Asks that every step of the workflow be scripted, which [run] checks.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
     */
    public fun ensureAllScripted(): AsyncStepScript

    /**
     * Runs the journey on [executor], as [AsyncScript.run] does.
     *
     * @throws TestMistake as [Script.run] says.
     * @throws Refusal as [Script.run] says.
     * @sample org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
     */
    @JvmSynthetic
    public suspend fun run(executor: AsyncLocalExecutor = AsyncLocalExecutor()): ScriptedRun
}
