@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync

/** Takes payments: the service a charge is paid through. */
fun interface PaymentGateway {
    fun take(amount: Long): String
}

fun requests(): StepDescriptor<Sync> {
    val amount = Requests.input("amount", TypeToken.of(Long::class.javaObjectType))
    val discount = Requests.optionalInput("discount", TypeToken.of(Long::class.javaObjectType))
    val payments = Requests.service("payments", PaymentGateway::class.java)
    val contributor = Requests.contributor()
    val reporter = Requests.stepReporter()
    return StepDescriptor.builder("charge") { got ->
            val charged = got[amount] - (got[discount] ?: 0)
            val gateway = got[payments]
            val contributions = got[contributor]
            val events = got[reporter]
            SyncStepRun {
                events.info("charging", charged)
                contributions.contribute("payment", gateway.take(charged))
                Outcome.success()
            }
        }
        .request(amount)
        .request(discount)
        .request(payments)
        .request(contributor)
        .request(reporter)
        .build()
}
