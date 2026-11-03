package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.testing.TestMistake

/**
 * A scripted run of a synchronous workflow before it names a step: it names steps, asks for every
 * step to be scripted, or runs. [AsyncScript] is its twin for asynchronous workflows.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
public interface Script {
    /**
     * Starts describing the step, or the rescue step, named [name].
     *
     * @throws TestMistake when the workflow has no step or rescue step named [name], or the script
     *   named it before.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun step(name: String): NamedStep<StepScript>

    /**
     * Asks that every step of the workflow be scripted, which [run] checks.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun ensureAllScripted(): Script

    /**
     * Runs the journey on a new [LocalExecutor], as [run] with an executor does.
     *
     * @throws TestMistake as [run] with an executor says.
     * @throws Refusal as [run] with an executor says.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    @Throws(Refusal::class) public fun run(): ScriptedRun

    /**
     * Runs the journey on [executor], playing the script, and returns the scripted run: the
     * journey's result and the record of every step.
     *
     * @throws TestMistake before the journey and any event, listing everything the script leaves
     *   out: for each scripted step, the first attempt it can have that is not described, and its
     *   rescue step, when it can fail on attempt 1 and the rescue step is not scripted, and, when
     *   every step was asked for, each step not scripted.
     * @throws Refusal when the executor refuses the instance.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    @Throws(Refusal::class) public fun run(executor: LocalExecutor): ScriptedRun
}

/**
 * A scripted run of a synchronous workflow once a step has described an attempt: it chooses another
 * attempt of the step, names the next step, asks for every step to be scripted, or runs.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
public interface StepScript : AttemptChoice<StepScript> {
    /**
     * Starts describing the next step, or rescue step, named [name].
     *
     * @throws TestMistake when the workflow has no step or rescue step named [name], or the script
     *   named it before.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun andStep(name: String): NamedStep<StepScript>

    /**
     * Asks that every step of the workflow be scripted, which [run] checks.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun ensureAllScripted(): StepScript

    /**
     * Runs the journey on a new [LocalExecutor], as [Script.run] does.
     *
     * @throws TestMistake as [Script.run] says.
     * @throws Refusal as [Script.run] says.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    @Throws(Refusal::class) public fun run(): ScriptedRun

    /**
     * Runs the journey on [executor], as [Script.run] does.
     *
     * @throws TestMistake as [Script.run] says.
     * @throws Refusal as [Script.run] says.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    @Throws(Refusal::class) public fun run(executor: LocalExecutor): ScriptedRun
}
