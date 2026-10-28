package org.itineradev.itinera.engine

import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The violations of how the input adapters of [descriptor] are attached: each name several share,
 * each adapter attached to no step, each of its steps several are attached to, and each adapter
 * attached to a step it does not have, once.
 */
internal fun adapterViolations(descriptor: WorkflowDescriptor<*, *>): List<Violation> {
    val steps = descriptor.steps.map(StepDescriptor<*>::name).toSet()
    val (known, unknown) =
        descriptor.inputAdapters.flatMap(::attachments).partition { attachment ->
            isAmong(attachment, steps)
        }
    return duplicateAdapterNames(descriptor.inputAdapters) +
        descriptor.inputAdapters.filter(::isUnattached).map(::unattached) +
        adaptedTwice(known) +
        unknown.map(::forUnknownStep).distinct()
}

private fun duplicateAdapterNames(adapters: List<InputAdapterDescriptor<*, *>>): List<Violation> =
    shared(adapters.map(InputAdapterDescriptor<*, *>::name)).map(Violation::DuplicateAdapterName)

private fun isUnattached(adapter: InputAdapterDescriptor<*, *>): Boolean = adapter.steps.isEmpty()

private fun unattached(adapter: InputAdapterDescriptor<*, *>): Violation =
    Violation.Misdeclared("input adapter ${adapter.name}", "is attached to no step")

private fun adaptedTwice(attached: List<Attached>): List<Violation> =
    attached
        .groupBy(Attached::step, Attached::adapter)
        .filterValues(::isAdaptedTwice)
        .map(::adaptedTwice)

/** One adapter attached once to one step. */
private class Attached(val step: StepName, val adapter: AdapterName)

private fun attachments(adapter: InputAdapterDescriptor<*, *>): List<Attached> =
    adapter.steps.map { step -> attached(step, adapter.name) }

private fun attached(step: StepName, adapter: AdapterName): Attached = Attached(step, adapter)

private fun isAdaptedTwice(adapters: List<AdapterName>): Boolean = isShared(adapters.size)

private fun adaptedTwice(adapted: Map.Entry<StepName, List<AdapterName>>): Violation =
    Violation.StepAdaptedTwice(adapted.key, adapted.value)

private fun isAmong(attachment: Attached, steps: Set<StepName>): Boolean = attachment.step in steps

private fun forUnknownStep(attachment: Attached): Violation =
    Violation.InputAdapterForUnknownStep(attachment.adapter, attachment.step)
