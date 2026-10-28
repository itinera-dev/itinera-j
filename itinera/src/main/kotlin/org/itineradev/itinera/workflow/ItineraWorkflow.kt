package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.InstanceBuilder

/**
 * A synchronous workflow written as a class: the application's own object, whose annotations
 * declare the workflow, [Workflow] and [WithStep] among them, and whose methods the workflow calls,
 * such as its [InputAdapter]s and its [IdGenerator]. Each journey runs one instance of it, made
 * with [instance]. The interface's other methods make what its input adapters return.
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

    /**
     * Supplies [value] to the input an input adapter is asked for: `null` is the value null.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.pricing
     */
    public fun supply(value: Any?): Supplied = Supplied.Value(value)

    /**
     * Leaves the input an input adapter is asked for to be read from the data bag.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.pricing
     */
    public fun fromDataBag(): Supplied = Supplied.FromDataBag
}
