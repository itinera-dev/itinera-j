package org.itineradev.itinera.engine

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The violations of how [descriptor] is put together, which need only its declaration. */
internal fun violationsOf(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    duplicateStepNames(descriptor.steps.map(StepDescriptor<*>::name)) +
        descriptor.steps.filter(::overdrawn).map(::overdrawnBudget) +
        serviceViolations(descriptor) +
        providerViolations(descriptor) +
        policyViolations(descriptor) +
        adapterViolations(descriptor)

/** A `duplicate step name` for each name shared by several of [names]. */
internal fun duplicateStepNames(names: List<StepName>): List<Violation> =
    shared(names).map(Violation::DuplicateStepName)

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
        unprovided(instance) +
        Modes(parts).of(instance) +
        initial.data.refused +
        initial.metadata.refused +
        reserved(initial)

private fun unchecked(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    if (descriptor is Checked) emptyList() else violationsOf(descriptor)

private fun unprovided(instance: WorkflowInstance<*>): List<Violation> =
    instance.descriptor.roles
        .filterNot { role -> role.isInstance(instance.workflow) }
        .map { role -> unprovided(instance.descriptor, role) }

private fun unprovided(descriptor: WorkflowDescriptor<*, *>, role: Class<*>): Violation =
    Violation.Misdeclared(
        "workflow ${descriptor.name}",
        "provides role ${role.name}, which its workflow object does not implement",
    )

private fun reserved(initial: Initial): List<Violation> =
    if (JOURNEY_ID_KEY in initial.metadata.values) listOf(Violation.ReservedMetadataKey)
    else emptyList()
