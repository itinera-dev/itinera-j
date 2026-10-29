package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.testing.TestMistake

/**
 * Which attempts of the step a script names last are described next: attempt 1 unless another is
 * chosen. Once they are described, the script goes on as [S].
 *
 * @param S what the script is once the attempts chosen are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public interface AttemptChoice<out S> {
    /**
     * Describes attempt [attempt], which comes after the last one described.
     *
     * @throws TestMistake when the step can never have attempt [attempt], it was described before,
     *   or an attempt before it is not described yet, naming only the first of these that holds.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun onAttempt(attempt: Int): AttemptScript<S>

    /**
     * Describes every attempt.
     *
     * @throws TestMistake when an attempt of the step was described before.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun onEveryAttempt(): AttemptScript<S>

    /**
     * Describes every attempt not yet described.
     *
     * @throws TestMistake when the step can have no attempt after those described, or its remaining
     *   attempts were described before.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun onRemainingAttempts(): AttemptScript<S>
}

/**
 * The step just named: its attempts are chosen, or attempt 1 is described.
 *
 * @param S what the script is once the attempts are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi public interface NamedStep<out S> : AttemptChoice<S>, AttemptScript<S>

/**
 * The attempts chosen, before they do anything: they run the real step, or do what follows, in this
 * order.
 *
 * @param S what the script is once the attempts are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public interface AttemptScript<out S> : AttemptContributing<S> {
    /**
     * The attempts run the real step.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willRunForReal(): S
}

/**
 * What the attempts chosen contribute, before what they emit.
 *
 * @param S what the script is once the attempts are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public interface AttemptContributing<out S> : AttemptEmitting<S> {
    /**
     * The attempts contribute [value] under [key], committed only if the attempt succeeds, as a
     * step's contributions are; a value that is not one aborts the journey, as it would for the
     * step.
     *
     * @throws TestMistake when the step declares no contributor, so the real step could not.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willContribute(key: String, value: Any?): AttemptContributing<S>
}

/**
 * What the attempts chosen emit through the step's reporter, before the global metadata they set.
 *
 * @param S what the script is once the attempts are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public interface AttemptEmitting<out S> : AttemptSettingGlobalMetadata<S> {
    /**
     * The attempts emit an event of [level] with [message], as the step's reporter would.
     *
     * @throws TestMistake when the step declares no step reporter, so the real step could not.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willEmit(level: Level, message: String): AttemptEmitting<S>

    /**
     * The attempts emit an event of [level] with [message] and [data], as the step's reporter
     * would.
     *
     * @throws TestMistake when the step declares no step reporter, so the real step could not.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willEmit(level: Level, message: String, data: Any?): AttemptEmitting<S>
}

/**
 * The global metadata the attempts chosen set through the step's reporter, before they end.
 *
 * @param S what the script is once the attempts are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public interface AttemptSettingGlobalMetadata<out S> : AttemptEndings<S> {
    /**
     * The attempts set the global metadata [key] to [value], as the step's reporter would.
     *
     * @throws TestMistake when the step declares no step reporter, so the real step could not.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willSetGlobalMetadata(key: String, value: Any?): AttemptSettingGlobalMetadata<S>
}

/**
 * How the attempts chosen end, with the consequences of the outcome a real step reports, or, for an
 * abnormal termination, of an error escaping its run.
 *
 * @param S what the script is once the attempts are described.
 * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
 */
@ExperimentalItineraApi
public interface AttemptEndings<out S> {
    /**
     * The attempts succeed.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willSucceed(): S

    /**
     * The attempts fail, for [reason].
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willFail(reason: Reason): S

    /**
     * The attempts fail, for a reason holding only [code].
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willFail(code: String): S

    /**
     * The attempts are skipped, for no reason.
     *
     * @throws TestMistake when the step is a rescue step, which never skips.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willSkip(): S

    /**
     * The attempts are skipped, for [reason].
     *
     * @throws TestMistake when the step is a rescue step, which never skips.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willSkipBecause(reason: Reason): S

    /**
     * The attempts are skipped, for a reason holding only [code].
     *
     * @throws TestMistake when the step is a rescue step, which never skips.
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willSkipBecause(code: String): S

    /**
     * The attempts terminate abnormally, with an exception carrying [errorMessage], as one escaping
     * the step's run does.
     *
     * @sample org.itineradev.itinera.executor.testing.samples.scriptedRuns
     */
    public fun willTerminateAbnormally(errorMessage: String): S
}
