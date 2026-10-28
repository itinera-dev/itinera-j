@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf

/** A shop that looks up the price of each payment, which takes a while. */
@Workflow(name = "priced returns")
@WithStep(AsyncUnpack::class, name = "unpack")
class AsyncPriced(private val shop: String) : ItineraAsyncWorkflow {
    /** Supplies the amount, looked up for the shop, and leaves every other input to the bag. */
    @InputAdapter(steps = ["unpack"], name = "prices")
    suspend fun lookedUp(@Key key: String): Supplied {
        delay(1)
        return if (key == "amount") supply(shop.length * 10L) else fromDataBag()
    }
}

fun asyncPricing(): InputAdapterDescriptor<AsyncPriced, Async> =
    asyncDescriptorOf(AsyncPriced::class).inputAdapters.single()
