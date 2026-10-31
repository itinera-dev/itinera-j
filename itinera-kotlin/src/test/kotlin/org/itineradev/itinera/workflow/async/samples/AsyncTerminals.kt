package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.service.async.AsyncProviderReporter
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.annotated.samples.PaymentGateway
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf

/** Settles the amount through the gateway the workflow lends it. */
@Step
class AsyncSettle(@Input private val amount: Long) : ItineraStep {
    suspend fun run(@Service gateway: PaymentGateway): Outcome {
        delay(1)
        gateway.take(amount)
        return succeed()
    }
}

/** A shop that connects to its payment gateway once per journey, which takes a while. */
@Workflow(name = "gateway orders")
@WithStep(AsyncSettle::class, name = "settle")
class AsyncTerminals(private val shop: String) : ItineraAsyncWorkflow {
    /** Connects to the shop's gateway, and says so. */
    @Provider
    suspend fun gateway(reporter: AsyncProviderReporter): Provided<PaymentGateway> {
        delay(1)
        reporter.info("connected")
        return provide(PaymentGateway { amount -> "$shop-$amount" })
    }
}

fun asyncProviding(): ProviderDescriptor<AsyncTerminals, Async> =
    asyncDescriptorOf(AsyncTerminals::class).providers.single()
