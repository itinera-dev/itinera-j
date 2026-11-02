package org.itineradev.itinera.request

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.samples.PaymentGateway
import org.itineradev.itinera.request.samples.requests
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.SyncStepFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequestsTest {
    @Test
    fun `a step descriptor keeps its requests in the order declared, each saying what it asks`() {
        assertEquals(
            listOf(
                "input amount: Long",
                "optional input discount: Long",
                "service payments: PaymentGateway",
                "contributor",
                "step reporter",
            ),
            requests().requests.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a step's factory reads each answer as its request's own type`() {
        val charge = requests()
        val contributed = mutableListOf<Pair<String, Any?>>()
        val reporter = Recording()
        val answers =
            charge.requests.zip(
                listOf(
                    40L,
                    5L,
                    PaymentGateway { amount -> "$amount paid" },
                    Contributor { key, value -> contributed += key to value },
                    reporter,
                )
            )
        (charge.factory as SyncStepFactory).build(Answers(answers.toMap())).run()
        assertEquals(listOf("payment" to "35 paid"), contributed)
        assertEquals(listOf("charging" to 35L), reporter.infos)
    }

    @Test
    fun `a service request names its key and its type`() {
        val payments = Requests.service("payments", PaymentGateway::class.java)
        assertEquals("payments", payments.key)
        assertEquals(PaymentGateway::class.java, payments.type)
    }

    private class Recording : StepReporter {
        val infos = mutableListOf<Pair<String, Any?>>()

        override fun info(message: String) {
            infos += message to null
        }

        override fun info(message: String, data: Any?) {
            infos += message to data
        }

        override fun warning(message: String) = Unit

        override fun warning(message: String, data: Any?) = Unit

        override fun error(message: String) = Unit

        override fun error(message: String, data: Any?) = Unit

        override fun setGlobalMetadata(key: String, value: Any?) = Unit
    }
}
