package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.value.TypeToken

/**
 * What a scripted run gives once its journey ends: the journey's [result], and a record of every
 * step and rescue step of the workflow, those the journey never reached included.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public class ScriptedRun
internal constructor(
    /** The journey's result, as the executor's `run` returns it. */
    public val result: JourneyResult,
    names: List<StepName>,
    built: List<Built>,
) {
    private val steps = names.associateBy(StepName::toString) { name -> StepRecord(name, built) }

    /**
     * The record of the step, or rescue step, named [name].
     *
     * @throws TestMistake when the workflow has no step or rescue step named [name].
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun step(name: String): StepRecord =
        steps[name] ?: throw TestMistake(listOf(Mistake.UnknownStep(name)))
}

/**
 * What a scripted run records of one step or rescue step: each attempt handed to the construction
 * of its step, scripted or real. An attempt that ended before its step's constructor was called,
 * because an input or a service could not be resolved, is not in it; one whose constructor failed
 * is.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public class StepRecord internal constructor(name: StepName, built: List<Built>) {
    /** Its attempts, in order. */
    public val attempts: List<AttemptRecord> =
        built.filter { entry -> isOf(entry, name) }.map(::AttemptRecord)

    /**
     * Its attempt numbered [number], or `null` when it has no such attempt, such as one its step
     * never reached, or 0.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun attempt(number: Int): AttemptRecord? = attempts.find { record ->
        isNumbered(record, number)
    }
}

/**
 * What a scripted run records of one attempt: its number, and the values its step was built with.
 *
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public class AttemptRecord internal constructor(built: Built) {
    /** The attempt it records. */
    public val attempt: Attempt = built.attempt

    private val values = DataBags.of(built.found)

    /**
     * Reads, as [type], the value under [key] that the step was built with: an input, or, for a
     * rescue step, a contribution it requested from the failed attempt. It reads as the data bag's
     * `read` does: present, absent or of another type.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun <T> input(key: String, type: TypeToken<T>): Read<T> = values.read(key, type)
}

private fun isOf(built: Built, step: StepName): Boolean = built.step == step

private fun isNumbered(record: AttemptRecord, number: Int): Boolean =
    record.attempt.number == number

/** An attempt handed to the construction of its step, with the values its requests [found]. */
internal class Built(val step: StepName, val attempt: Attempt, val found: Map<String, Captured>)
