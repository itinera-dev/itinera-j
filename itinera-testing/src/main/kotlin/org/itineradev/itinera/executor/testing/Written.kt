package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.engine.rescueOf
import org.itineradev.itinera.engine.stepNamesOf
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor

/** A script for a journey of [instance], as written so far, in either mode. */
internal class Written<M : Mode>(private val instance: WorkflowInstance<M>) {
    private val descriptor = instance.descriptor
    private val targets = descriptor.steps.flatMap(::targetsOf)
    private val named = mutableListOf<Target<*>>()
    private var all = false

    /**
     * The step, or rescue step, named [name], which the script names next.
     *
     * @throws TestMistake when the workflow has no step or rescue step named [name], or the script
     *   named it before.
     */
    fun step(name: String): Target<*> {
        val target =
            targets.find { target -> hasName(target, name) }
                ?: throw TestMistake(listOf(Mistake.UnknownStep(name)))
        if (target in named) throw TestMistake(listOf(Mistake.StepScriptedTwice(name)))
        named += target
        return target
    }

    /** Asks that every step be scripted. */
    fun ensureAllScripted() {
        all = true
    }

    /**
     * What plays the script, once nothing is left out, with the instance it plays on.
     *
     * @throws TestMistake listing everything the script leaves out.
     */
    fun playing(): Pair<Playing, WorkflowInstance<M>> {
        val leftOut = named.flatMap(::leftOutOf) + unscripted()
        if (leftOut.isNotEmpty()) throw TestMistake(leftOut)
        val playing = Playing(named.toList(), stepNamesOf(descriptor))
        return playing to ScriptedJourney(instance, playing)
    }

    private fun leftOutOf(target: Target<*>): List<Mistake> =
        listOfNotNull(attemptLeftOut(target), rescueLeftOut(target))

    private fun rescueLeftOut(target: Target<*>): Mistake? {
        val rescue = (target as? StepTarget)?.rescue ?: return null
        return if (target.canFailFirst() && rescue !in named) {
            Mistake.RescueStepLeftOut(rescue.name.toString(), target.name.toString())
        } else {
            null
        }
    }

    private fun unscripted(): List<Mistake> =
        if (all)
            descriptor.steps.map(StepDescriptor<M>::name).filterNot(::isScripted).map(::stepLeftOut)
        else emptyList()

    private fun isScripted(step: StepName): Boolean = named.any { target -> isNamed(target, step) }
}

private fun attemptLeftOut(target: Target<*>): Mistake? {
    val attempt = target.firstLeftOut() ?: return null
    return Mistake.AttemptLeftOut(target.name.toString(), attempt)
}

private fun stepLeftOut(step: StepName): Mistake = Mistake.StepLeftOut(step.toString())

private fun hasName(target: Target<*>, name: String): Boolean = target.name.toString() == name

private fun targetsOf(step: StepDescriptor<*>): List<Target<*>> {
    val rescue = step.rescue ?: return listOf(stepTarget(step, null))
    val target = RescueTarget(rescueOf(step.name), rescue.requests)
    return listOf(stepTarget(step, target), target)
}

private fun stepTarget(step: StepDescriptor<*>, rescue: RescueTarget?): StepTarget =
    StepTarget(step.name, step.requests, step.retryBudget, step.retryOnAbnormalTermination, rescue)
