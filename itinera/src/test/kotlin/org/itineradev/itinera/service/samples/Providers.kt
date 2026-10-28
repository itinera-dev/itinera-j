@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.service.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.samples.PaymentGateway
import org.itineradev.itinera.request.samples.requests
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.samples.Orders

private val JOURNEY_ID = Requests.journeyId()
private val REPORTER = Requests.providerReporter()
private val STEP = Requests.stepName()
private val ATTEMPT = Requests.attempt()

/** A gateway that takes payments for one shop, under the journey's ID. */
private class ShopGateway(private val shop: String, private val journey: String) : PaymentGateway {
    override fun take(amount: Long): String = "$shop-$journey-$amount"
}

/** The shop's gateway, supplied with the shop's name, or none for a shop with no name. */
private fun forShop(orders: Orders, got: Got<JourneyProviderKind<Sync>>): Provided<PaymentGateway> {
    got[REPORTER].info("opening the gateway")
    return if (orders.shop.isEmpty()) Provided.NoInstance
    else Provided.Instance(ShopGateway(orders.shop, got[JOURNEY_ID].toString()), orders.shop)
}

fun providers(): WorkflowDescriptor<Orders, Sync> {
    val gateway =
        ProviderDescriptor.builder<Orders, PaymentGateway>(
                "gateway",
                "payments",
                PaymentGateway::class.java,
            ) { orders, got ->
                forShop(orders, got)
            }
            .request(JOURNEY_ID)
            .request(REPORTER)
            .build()
    val retried =
        ProviderDescriptor.perAttempt<Orders, PaymentGateway>(
                "retried gateway",
                "payments",
                PaymentGateway::class.java,
            ) { _, got ->
                Provided.Instance(ShopGateway(got[STEP].toString(), got[ATTEMPT].toString()))
            }
            .step("charge")
            .request(STEP)
            .request(ATTEMPT)
            .build()
    return WorkflowDescriptor.builder(Orders::class.java, "orders")
        .step(requests())
        .provider(gateway)
        .provider(retried)
        .build()
}

fun misprovided(): List<Violation> {
    val labels =
        ProviderDescriptor.builder<Orders, String>("gateway", "payments", String::class.java) { _, _
                ->
                Provided.Instance("label")
            }
            .build()
    val courier =
        ProviderDescriptor.perAttempt<Orders, String>("courier", "couriers", String::class.java) {
                _,
                _ ->
                Provided.Instance("courier")
            }
            .step("ship")
            .build()
    return try {
        WorkflowDescriptor.builder(Orders::class.java, "orders")
            .step(requests())
            .provider(labels)
            .provider(labels)
            .provider(courier)
            .build()
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }
}
