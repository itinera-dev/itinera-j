package org.itineradev.itinera.service.async

import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder

/** What [ProviderModeTypesTest] refuses, written so that it compiles. */
internal object ProviderModeTypesTwins {
    class Shop

    val labels =
        ProviderDescriptor.builder<Shop, String>("labels", "labels", String::class.java) { _, _ ->
                Provided.NoInstance
            }
            .build()
    val asyncLabels =
        asyncProvider<Shop, String>("labels", "labels", String::class.java) { _, _ ->
                Provided.NoInstance
            }
            .build()

    val shop = WorkflowDescriptor.builder(Shop::class.java, "shop").provider(labels)
    val asyncShop = asyncBuilder<Shop>("shop").provider(asyncLabels)
}
