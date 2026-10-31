@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.journey.kotlin.read
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** A shop that works out what each payment charges from the price of the order. */
@Workflow(name = "priced orders")
@WithStep(Pay::class, name = "pay")
class Priced : ItineraWorkflow {
    /**
     * Supplies the amount, the price less any discount, and leaves every other input to the bag.
     */
    @InputAdapter(steps = ["pay"])
    fun pricing(@Key key: String, @FromWorkflow price: Long, access: DataBagAccess): Supplied =
        if (key == "amount") supply(price - (access.read<Long>("discount").orNull() ?: 0L))
        else fromDataBag()
}

fun pricing(): InputAdapterDescriptor<Priced, Sync> =
    WorkflowDescriptor.of(Priced::class.java).inputAdapters.single()
