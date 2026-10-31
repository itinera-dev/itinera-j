@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.async.asyncInputAdapter

private val KEY = Requests.key()

/** Looks up the price of an order, which takes a while, and supplies it as the amount. */
private suspend fun lookedUp(orders: AsyncOrders, key: String): Supplied {
    delay(1)
    return if (key == "amount") Supplied.Value(orders.shop.length * 10L) else Supplied.FromDataBag
}

fun asyncAdapters(): InputAdapterDescriptor<AsyncOrders, Async> =
    asyncInputAdapter<AsyncOrders>("pricing", "charge") { orders, got ->
            lookedUp(orders, got[KEY])
        }
        .request(KEY)
        .build()
