package org.itineradev.itinera.workflow

import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun

/** What [BuilderTypesTest] refuses, written so that it compiles. */
internal object BuilderTypesTwins {
    class Shop

    val shop = WorkflowDescriptor.builder(Shop::class.java, "shop")
    val succeeding = SyncStepRun { Outcome.success() }
    val pack = StepDescriptor.builder("pack") { succeeding }
    val ids = shop.idGenerator { _, _ -> "order-1" }

    fun add(shop: WorkflowBuilder<Shop, Sync>, step: StepDescriptor<Sync>) = shop.step(step)

    fun addAsync(shop: WorkflowBuilder<Shop, Async>, step: StepDescriptor<Async>) = shop.step(step)
}
