package org.itineradev.itinera.workflow.async

import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** What [AdapterModeTypesTest] refuses, written so that it compiles. */
internal object AdapterModeTypesTwins {
    class Shop

    val pricing =
        InputAdapterDescriptor.builder<Shop>("pricing", "charge") { _, _ -> Supplied.FromDataBag }
            .build()
    val asyncPricing =
        asyncInputAdapter<Shop>("pricing", "charge") { _, _ -> Supplied.FromDataBag }.build()

    val shop = WorkflowDescriptor.builder(Shop::class.java, "shop").inputAdapter(pricing)
    val asyncShop = asyncBuilder<Shop>("shop").inputAdapter(asyncPricing)
}
