@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.service.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncProviderReporter
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.async.AsyncProviderReporter
import org.itineradev.itinera.service.async.asyncPerAttemptProvider
import org.itineradev.itinera.service.async.asyncProvider
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.async.samples.AsyncOrders

private val REPORTER = Requests.asyncProviderReporter()
private val ATTEMPT = Requests.attempt()

/** Takes payments: the service a charge is paid through. */
fun interface Payments {
    fun take(amount: Long): String
}

/** Connects to the shop's gateway, which takes a while, and says so. */
private suspend fun connected(orders: AsyncOrders, reporter: AsyncProviderReporter): Payments {
    delay(1)
    reporter.info("connected")
    return Payments { amount -> "${orders.shop}-$amount" }
}

fun asyncProviders(): List<ProviderDescriptor<AsyncOrders, Async>> =
    listOf(
        asyncProvider<AsyncOrders, Payments>("gateway", "payments", Payments::class.java) {
                orders,
                got ->
                Provided.Instance(connected(orders, got[REPORTER]))
            }
            .request(REPORTER)
            .build(),
        asyncPerAttemptProvider<AsyncOrders, Payments>(
                "retried gateway",
                "payments",
                Payments::class.java,
            ) { _, got ->
                delay(1)
                Provided.Instance(Payments { amount -> "${got[ATTEMPT]}-$amount" })
            }
            .step("charge")
            .request(ATTEMPT)
            .build(),
    )
