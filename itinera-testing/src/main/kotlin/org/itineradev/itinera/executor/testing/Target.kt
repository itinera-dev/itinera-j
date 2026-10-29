package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.engine.declaresContributor
import org.itineradev.itinera.engine.declaresStepReporter
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.RescueEnding
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake

/**
 * A step, or a rescue step, the script names, with what the script says of its attempts so far,
 * each ending as an [E].
 */
internal sealed class Target<E>(val name: StepName, requests: List<Request<*, *>>) {
    private val numbered = mutableListOf<Described<E>>()
    private var rest: Described<E>? = null

    /** Whether its run declares a contributor. */
    val contributes: Boolean = declaresContributor(requests)

    /** Whether its run declares a step reporter. */
    val reports: Boolean = declaresStepReporter(requests)

    /** The most attempts it can have. */
    abstract val mostAttempts: Int

    /** How many attempts are described by number. */
    val described: Int
        get() = numbered.size

    /** Whether every attempt after those described by number is described. */
    val restDescribed: Boolean
        get() = rest != null

    /** What the script says attempt [number] does, if it says. */
    fun described(number: Int): Described<E>? = numbered.getOrNull(number - 1) ?: rest

    /**
     * Says that the next attempt by number, or every remaining one when [remaining], runs for real.
     */
    fun runsForReal(remaining: Boolean) {
        describe(remaining, RealRun)
    }

    /**
     * Says that the next attempt by number, or every remaining one when [remaining], plays
     * [actions], then ends as [ending] says.
     *
     * @throws TestMistake when it cannot end so.
     */
    fun plays(remaining: Boolean, actions: List<Action>, ending: Ending) {
        describe(remaining, Played(actions, endingOf(ending)))
    }

    private fun describe(remaining: Boolean, what: Described<E>) {
        if (remaining) rest = what else numbered += what
    }

    /**
     * The first attempt it can have that the script leaves out, if any: attempts follow one another
     * while the script says they may, and stop at the first it does not describe.
     */
    fun firstLeftOut(): Int? = (1..mostAttempts).find(::stopsAt)?.takeUnless(::isDescribed)

    private fun stopsAt(attempt: Int): Boolean {
        val what = described(attempt) ?: return true
        return !continuesAfter(attempt, what)
    }

    private fun isDescribed(attempt: Int): Boolean = described(attempt) != null

    /** Whether an attempt may follow attempt [attempt], described as [what], budget aside. */
    protected abstract fun continuesAfter(attempt: Int, what: Described<E>): Boolean

    /**
     * Its own ending for [ending].
     *
     * @throws TestMistake when it cannot end so.
     */
    protected abstract fun endingOf(ending: Ending): E
}

/** A step the script names, under its [budget], its [retryOnAbnormal] setting and its [rescue]. */
internal class StepTarget(
    name: StepName,
    requests: List<Request<*, *>>,
    private val budget: Int,
    private val retryOnAbnormal: Boolean,
    /** Its rescue step's name, if it has one. */
    val rescue: StepName?,
) : Target<Ending>(name, requests) {
    override val mostAttempts: Int
        get() = budget + 1 + if (rescue == null) 0 else 1

    override fun continuesAfter(attempt: Int, what: Described<Ending>): Boolean =
        when (what) {
            RealRun -> true
            is Played -> continuesAfter(attempt, what.ending)
        }

    private fun continuesAfter(attempt: Int, ending: Ending): Boolean =
        when (ending) {
            Ending.Success,
            is Ending.Skipped -> false
            is Ending.Failure -> true
            is Ending.AbnormalTermination -> retryOnAbnormal || rescuedAfter(attempt)
        }

    private fun rescuedAfter(attempt: Int): Boolean = attempt == 1 && rescue != null

    /** Whether attempt 1, as described, can fail, so the rescue step may run. */
    fun canFailFirst(): Boolean =
        when (val first = described(1)) {
            null -> false
            RealRun -> true
            is Played -> canFail(first.ending)
        }

    override fun endingOf(ending: Ending): Ending = ending
}

/** A rescue step the script names, which has one attempt. */
internal class RescueTarget(name: StepName, requests: List<Request<*, *>>) :
    Target<RescueEnding>(name, requests) {
    override val mostAttempts: Int
        get() = 1

    override fun continuesAfter(attempt: Int, what: Described<RescueEnding>): Boolean = false

    override fun endingOf(ending: Ending): RescueEnding =
        when (ending) {
            Ending.Success -> RescueEnding.Success
            is Ending.Failure -> RescueEnding.Failure(ending.reason)
            is Ending.AbnormalTermination -> RescueEnding.AbnormalTermination(ending.errorMessage)
            is Ending.Skipped -> throw TestMistake(listOf(Mistake.RescueStepSkips(name.toString())))
        }
}

private fun canFail(ending: Ending): Boolean =
    when (ending) {
        Ending.Success,
        is Ending.Skipped -> false
        is Ending.Failure,
        is Ending.AbnormalTermination -> true
    }

/** What the script says of one attempt: the real step runs, or the attempt plays. */
internal sealed interface Described<out E>

/** The attempt runs the real step. */
internal data object RealRun : Described<Nothing>

/** The attempt plays [actions], in order, then ends with [ending]. */
internal class Played<out E>(val actions: List<Action>, val ending: E) : Described<E>

/** What a scripted attempt does before it ends. */
internal sealed interface Action {
    /** Contributes [value] under [key]. */
    class Contribute(val key: String, val value: Any?) : Action

    /** Emits an event of [level] with [message] and [data]. */
    class Emit(val level: Level, val message: String, val data: Any?) : Action

    /** Sets the global metadata [key] to [value]. */
    class SetGlobalMetadata(val key: String, val value: Any?) : Action
}
