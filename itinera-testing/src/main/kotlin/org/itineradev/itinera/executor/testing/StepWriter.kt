package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake

/**
 * A scripted run being written, describing the attempts of the step it last named, [target]:
 * attempt 1 until another is chosen. Once attempts are described, it goes on as [next]. Each choice
 * of attempts is described once: describing it again, through a reference a test kept, is the
 * mistake `attempt scripted twice`, naming the attempt chosen.
 */
@Suppress("TooManyFunctions")
internal abstract class StepWriter<S>(private val target: Target<*>) : NamedStep<S> {
    private var remaining = false
    private var chosen = 1
    private var open = true
    private val actions = mutableListOf<Action>()

    /** What the script is once attempts are described. */
    protected abstract val next: S

    override fun onAttempt(attempt: Int): AttemptScript<S> {
        mistakeIn(attempt)?.let(::mistaken)
        choose(attempt, remaining = false)
        return this
    }

    private fun mistakeIn(attempt: Int): Mistake? {
        val step = target.name.toString()
        val next = target.described + 1
        return when {
            attempt !in 1..target.mostAttempts -> Mistake.NoSuchAttempt(step, attempt)
            restChosen || attempt < next -> Mistake.AttemptScriptedTwice(step, attempt)
            attempt > next -> Mistake.GapInTheAttempts(step, attempt)
            else -> null
        }
    }

    override fun onEveryAttempt(): AttemptScript<S> {
        if (target.described > 0 || target.restDescribed) {
            mistaken(Mistake.AttemptScriptedTwice(target.name.toString(), 1))
        }
        choose(1, remaining = true)
        return this
    }

    override fun onRemainingAttempts(): AttemptScript<S> {
        val next = target.described + 1
        if (next > target.mostAttempts) {
            mistaken(Mistake.NoSuchAttempt(target.name.toString(), next))
        }
        if (restChosen) mistaken(Mistake.AttemptScriptedTwice(target.name.toString(), next))
        choose(next, remaining = true)
        return this
    }

    /** Whether every or the remaining attempts were chosen, described or not yet. */
    private val restChosen: Boolean
        get() = target.restDescribed || (open && remaining)

    private fun choose(attempt: Int, remaining: Boolean) {
        this.remaining = remaining
        chosen = attempt
        open = true
    }

    private fun describing() {
        if (!open) mistaken(Mistake.AttemptScriptedTwice(target.name.toString(), chosen))
    }

    override fun willRunForReal(): S {
        describing()
        target.runsForReal(remaining)
        return closed()
    }

    override fun willContribute(key: String, value: Any?): AttemptContributing<S> {
        describing()
        if (!target.contributes) mistaken(Mistake.NoContributor(target.name.toString()))
        actions += Action.Contribute(key, value)
        return this
    }

    override fun willEmit(level: Level, message: String): AttemptEmitting<S> =
        willEmit(level, message, null)

    override fun willEmit(level: Level, message: String, data: Any?): AttemptEmitting<S> {
        describing()
        reports()
        actions += Action.Emit(level, message, data)
        return this
    }

    override fun willSetGlobalMetadata(key: String, value: Any?): AttemptSettingGlobalMetadata<S> {
        describing()
        reports()
        actions += Action.SetGlobalMetadata(key, value)
        return this
    }

    private fun reports() {
        if (!target.reports) mistaken(Mistake.NoReporter(target.name.toString()))
    }

    override fun willSucceed(): S = ended(Ending.success())

    override fun willFail(reason: Reason): S = ended(Ending.failure(reason))

    override fun willFail(code: String): S = ended(Ending.failure(code))

    override fun willSkip(): S = ended(Ending.skipped())

    override fun willSkipBecause(reason: Reason): S = ended(Ending.skipped(reason))

    override fun willSkipBecause(code: String): S = ended(Ending.skipped(code))

    override fun willTerminateAbnormally(errorMessage: String): S =
        ended(Ending.abnormalTermination(errorMessage))

    private fun ended(ending: Ending): S {
        describing()
        target.plays(remaining, actions.toList(), ending)
        return closed()
    }

    private fun closed(): S {
        actions.clear()
        open = false
        return next
    }
}

private fun mistaken(mistake: Mistake): Nothing = throw TestMistake(listOf(mistake))
