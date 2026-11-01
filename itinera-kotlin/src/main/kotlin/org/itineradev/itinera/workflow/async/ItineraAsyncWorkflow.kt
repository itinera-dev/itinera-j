package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.instance.async.AsyncInstanceBuilder
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow

/**
 * An asynchronous workflow written as a class: the application's own object, whose annotations
 * declare the workflow, [Workflow] and [WithStep] among them, and whose methods the workflow calls,
 * such as its [InputAdapter]s, its service providers and its [IdGenerator], which stays
 * synchronous. Every step's `run`, every input adapter and every service provider is a `suspend`
 * function, and the interface's other methods make what its input adapters and its service
 * providers return. Each journey runs one instance of it, made with [instance].
 *
 * @sample org.itineradev.itinera.workflow.async.samples.asyncShipping
 */
public interface ItineraAsyncWorkflow {
    /**
     * Starts a workflow instance of this object, reading the class's annotations the first time any
     * of its objects is asked.
     *
     * @throws Violations with every mistake in the class's declaration.
     * @sample org.itineradev.itinera.workflow.async.samples.asyncShipping
     */
    public fun instance(): AsyncInstanceBuilder =
        AsyncInstanceBuilder.of(asyncDescriptorOf(javaClass.kotlin), this)

    /**
     * Supplies [value] to the input an input adapter is asked for: `null` is the value null.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncPricing
     */
    public fun supply(value: Any?): Supplied = Supplied.Value(value)

    /**
     * Leaves the input an input adapter is asked for to be read from the data bag.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncPricing
     */
    public fun fromDataBag(): Supplied = Supplied.FromDataBag

    /**
     * Lends [instance] to the steps a service provider serves, supplied with no data.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncProviding
     */
    public fun <T : Any> provide(instance: T): Provided<T> = Provided.Instance(instance)

    /**
     * Lends [instance] to the steps a service provider serves, supplied with [data], which must be
     * a value.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncProviding
     */
    public fun <T : Any> provide(instance: T, data: Any?): Provided<T> =
        Provided.Instance(instance, data)

    /**
     * Provides no instance at all, which fails the step's building.
     *
     * @sample org.itineradev.itinera.workflow.async.samples.asyncProviding
     */
    public fun <T : Any> noInstance(): Provided<T> = Provided.NoInstance
}
