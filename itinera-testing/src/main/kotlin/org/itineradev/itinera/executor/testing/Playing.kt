package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.Lent
import org.itineradev.itinera.engine.Play
import org.itineradev.itinera.engine.ScriptedInstance
import org.itineradev.itinera.engine.Scripting
import org.itineradev.itinera.engine.Severity
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.RescueEnding
import org.itineradev.itinera.workflow.Mode

/**
 * What the engine plays for a script naming [targets], recording every attempt handed to the
 * construction of its step, for a record of the steps and rescue steps [names].
 */
internal class Playing(targets: List<Target<*>>, private val names: List<StepName>) : Scripting {
    private val steps = targets.filterIsInstance<StepTarget>()
    private val rescues = targets.filterIsInstance<RescueTarget>()
    private val built = mutableListOf<Built>()

    @Synchronized
    override fun building(step: StepName, attempt: Attempt, found: Map<String, Captured>) {
        built += Built(step, attempt, found)
    }

    override fun step(step: StepName, attempt: Attempt): Play<Outcome>? =
        when (
            val what = steps.find { target -> isNamed(target, step) }?.described(attempt.number)
        ) {
            is Played -> stepPlay(what)
            RealRun,
            null -> null
        }

    override fun rescue(rescue: StepName): Play<RescueOutcome>? =
        when (val what = rescues.find { target -> isNamed(target, rescue) }?.described(1)) {
            is Played -> rescuePlay(what)
            RealRun,
            null -> null
        }

    /** The scripted run of a journey that ended with [result]. */
    @Synchronized
    fun ran(result: JourneyResult): ScriptedRun = ScriptedRun(result, names, built.toList())
}

/** [instance], whose journey runs as [scripting] says. */
internal class ScriptedJourney<M : Mode>(
    private val instance: WorkflowInstance<M>,
    override val scripting: Scripting,
) : ScriptedInstance<M>, WorkflowInstance<M> by instance

/** Whether [target] is named [name]. */
internal fun isNamed(target: Target<*>, name: StepName): Boolean = target.name == name

private fun stepPlay(played: Played<Ending>): Play<Outcome> = Play { lent ->
    act(played.actions, lent)
    played.ending.outcome()
}

private fun rescuePlay(played: Played<RescueEnding>): Play<RescueOutcome> = Play { lent ->
    act(played.actions, lent)
    played.ending.outcome()
}

private suspend fun act(actions: List<Action>, lent: Lent) {
    for (action in actions) act(action, lent)
}

private suspend fun act(action: Action, lent: Lent) {
    when (action) {
        is Action.Contribute -> lent.contributor.contribute(action.key, action.value)
        is Action.Emit -> lent.emits.emit(severityOf(action.level), action.message, action.data)
        is Action.SetGlobalMetadata -> lent.emits.setGlobalMetadata(action.key, action.value)
    }
}

private fun severityOf(level: Level): Severity =
    when (level) {
        Level.INFO -> Severity.INFO
        Level.WARNING -> Severity.WARNING
        Level.ERROR -> Severity.ERROR
    }
