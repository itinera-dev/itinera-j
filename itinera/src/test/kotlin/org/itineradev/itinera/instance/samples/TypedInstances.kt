package org.itineradev.itinera.instance.samples

import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.samples.Orders
import org.itineradev.itinera.workflow.samples.ids

fun typedInstances(): WorkflowInstance<Sync> {
    val instance =
        InstanceBuilder.of(ids(), Orders("corner")).initialData(Order("42", 40L)).create()
    check(instance.initialData == mapOf("order" to "42", "price" to 40L))
    return instance
}
