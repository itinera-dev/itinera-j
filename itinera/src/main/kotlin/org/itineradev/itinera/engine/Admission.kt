package org.itineradev.itinera.engine

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The violations of how [descriptor] is put together, which need only its declaration. */
internal fun violationsOf(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    duplicateStepNames(descriptor.steps.map(StepDescriptor<*>::name)) +
        descriptor.steps.filter(::overdrawn).map(::overdrawnBudget)

/** A `duplicate step name` for each name shared by several of [names]. */
internal fun duplicateStepNames(names: List<StepName>): List<Violation> =
    names
        .groupingBy(::itself)
        .eachCount()
        .filterValues(::isShared)
        .keys
        .map(Violation::DuplicateStepName)

private fun itself(name: StepName): StepName = name

private fun isShared(count: Int): Boolean = count > 1

private fun overdrawn(step: StepDescriptor<*>): Boolean = step.retryBudget < 0

private fun overdrawnBudget(step: StepDescriptor<*>): Violation =
    Violation.Misdeclared("step ${step.name}", "has a retry budget below 0")

/** The key under which the executor sets the journey ID as global metadata. */
internal const val JOURNEY_ID_KEY: String = "journey_id"

/**
 * Every violation that keeps [instance] from running on the executor of [parts]: those of a
 * descriptor that was not checked when made, every part of another mode, and the initial values
 * refused.
 */
internal fun <M : Mode> admission(
    instance: WorkflowInstance<M>,
    parts: Parts<M>,
    initial: Initial,
): List<Violation> =
    unchecked(instance.descriptor) +
        Modes(parts).of(instance) +
        initial.data.refused +
        initial.metadata.refused +
        reserved(initial)

private fun unchecked(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    if (descriptor is Checked) emptyList() else violationsOf(descriptor)

private fun reserved(initial: Initial): List<Violation> =
    if (JOURNEY_ID_KEY in initial.metadata.values) listOf(Violation.ReservedMetadataKey)
    else emptyList()

/** Finds every part of an instance in a mode the executor of [parts] does not accept. */
private class Modes<M : Mode>(private val parts: Parts<M>) {
    fun of(instance: WorkflowInstance<M>): List<Violation> =
        workflow(instance.descriptor) +
            instance.descriptor.steps.filterNot(::accepts).map(::step) +
            instance.reporters.withIndex().filterNot(::accepts).map(::reporter)

    private fun workflow(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
        if (descriptor.mode == parts.mode) emptyList()
        else listOf(Violation.ModeNotAccepted("workflow ${descriptor.name}", parts.mode))

    private fun accepts(step: StepDescriptor<*>): Boolean = parts.accepts(step.factory)

    private fun accepts(reporter: IndexedValue<ReporterOf<*>?>): Boolean =
        parts.accepts(reporter.value)

    private fun step(step: StepDescriptor<*>): Violation =
        Violation.ModeNotAccepted("step ${step.name}", parts.mode)

    private fun reporter(reporter: IndexedValue<ReporterOf<*>?>): Violation =
        Violation.ModeNotAccepted("reporter ${reporter.index + 1}", parts.mode)
}
