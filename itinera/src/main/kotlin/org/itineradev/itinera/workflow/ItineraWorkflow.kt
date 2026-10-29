package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.service.Provided

/**
 * A synchronous workflow written as a class: the application's own object, whose annotations
 * declare the workflow, [Workflow] and [WithStep] among them, and whose methods the workflow calls,
 * such as its [InputAdapter]s, its service providers and its [IdGenerator]. Each journey runs one
 * instance of it, made with [instance]. The interface's other methods make what its input adapters
 * and its service providers return.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
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

    /**
     * Lends [instance] to the steps a service provider serves, supplied with no data.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.providing
     */
    @ExperimentalItineraApi
    public fun <T : Any> provide(instance: T): Provided<T> = Provided.Instance(instance)

    /**
     * Lends [instance] to the steps a service provider serves, supplied with [data], which must be
     * a value.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.providing
     */
    @ExperimentalItineraApi
    public fun <T : Any> provide(instance: T, data: Any?): Provided<T> =
        Provided.Instance(instance, data)

    /**
     * Provides no instance at all, which fails the step's building.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.providing
     */
    @ExperimentalItineraApi public fun <T : Any> noInstance(): Provided<T> = Provided.NoInstance
}
