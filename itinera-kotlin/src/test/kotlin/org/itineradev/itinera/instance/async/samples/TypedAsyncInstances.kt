package org.itineradev.itinera.instance.async.samples

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.async.AsyncInstanceBuilder
import org.itineradev.itinera.journey.Datum
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.itineradev.itinera.workflow.async.samples.asyncBuilding

fun typedAsyncInstances(): WorkflowInstance<Async> {
    /** An order, with its amount under "price" and a discount that may be absent. */
    data class Order(val order: String, @Datum(key = "price") val amount: Long, val discount: Long?)

    val instance =
        AsyncInstanceBuilder.of(asyncBuilding(), AsyncOrders("corner"))
            .initialData(Order("42", 40L, null))
            .create()
    check(instance.initialData == mapOf("order" to "42", "price" to 40L))
    return instance
}
