package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.async.AsyncInstanceBuilder
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow

/**
 * An asynchronous workflow written as a class: the application's own object, whose annotations
 * declare the workflow, [Workflow] and [WithStep] among them, and whose methods the workflow calls,
 * such as its [InputAdapter]s and its [IdGenerator], which stays synchronous. Every step's `run`
 * and every input adapter is a `suspend` function, and the interface's other methods make what its
 * input adapters return. Each journey runs one instance of it, made with [instance].
 *
 * @sample org.itineradev.itinera.workflow.async.samples.asyncShipping
 */
@ExperimentalItineraApi
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
}
