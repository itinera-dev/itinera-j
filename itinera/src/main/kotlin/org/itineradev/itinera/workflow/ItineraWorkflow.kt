package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.InstanceBuilder

/**
 * A synchronous workflow written as a class: the application's own object, whose annotations
 * declare the workflow, [Workflow] and [WithStep] among them, and whose methods the workflow calls,
 * such as its [IdGenerator]. Each journey runs one instance of it, made with [instance].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@ExperimentalItineraApi
public interface ItineraWorkflow {
    /**
     * Starts a workflow instance of this object, reading the class's annotations the first time any
     * of its objects is asked.
     *
     * @throws Violations with every mistake in the class's declaration.
     * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
     */
    public fun instance(): InstanceBuilder =
        InstanceBuilder.of(WorkflowDescriptor.of(javaClass), this)
}
