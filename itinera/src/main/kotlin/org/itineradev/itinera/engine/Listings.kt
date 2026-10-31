package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ListedRescue
import org.itineradev.itinera.workflow.ListedService
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.Position
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The listing of [descriptor], read from its declaration alone, step by step in order. */
internal fun listingOf(descriptor: WorkflowDescriptor<*, *>): List<ListedStep> =
    descriptor.steps.mapIndexed(Listing(descriptor)::listed)

private class Listing(private val descriptor: WorkflowDescriptor<*, *>) {
    fun listed(index: Int, step: StepDescriptor<*>): ListedStep =
        ListedStep(
            step.name,
            Position.of(index + 1),
            step.policies.map(StepPolicyDescriptor<*, *>::name),
            adapterOf(step.name),
            servicesOf(step.name, step.requests),
            rescueSectionOf(step),
        )

    private fun rescueSectionOf(step: StepDescriptor<*>): ListedRescue? {
        val rescue = step.rescue ?: return null
        val name = rescueOf(step.name)
        return ListedRescue(name, adapterOf(name), servicesOf(name, rescue.requests))
    }

    private fun adapterOf(step: StepName): AdapterName? =
        descriptor.inputAdapters.find { adapter -> isAttachedTo(adapter, step) }?.name

    private fun servicesOf(step: StepName, requests: List<Request<*, *>>): List<ListedService> =
        servicesAskedBy(requests).mapNotNull { service -> listed(step, service.key) }

    private fun listed(step: StepName, key: String): ListedService? {
        val provider = servingOf(step, key, descriptor.providers) ?: return null
        return ListedService(key, provider.name)
    }
}

private fun isAttachedTo(adapter: InputAdapterDescriptor<*, *>, step: StepName): Boolean =
    step in adapter.steps
