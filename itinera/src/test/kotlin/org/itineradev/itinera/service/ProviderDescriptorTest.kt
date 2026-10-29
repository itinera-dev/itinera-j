package org.itineradev.itinera.service

import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.samples.PaymentGateway
import org.itineradev.itinera.service.samples.providerReporters
import org.itineradev.itinera.service.samples.providers
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.samples.Orders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class ProviderDescriptorTest {
    @Test
    fun `a built provider keeps its name, key, type, steps and requests, in order`() {
        val (gateway, retried) = providers().providers
        assertEquals(
            listOf("gateway", "payments", PaymentGateway::class.java, emptyList<StepName>()),
            listOf(gateway.name.toString(), gateway.key, gateway.type, gateway.steps),
        )
        assertEquals(listOf(StepName.of("charge")), retried.steps)
        assertEquals(
            listOf("journey ID", "provider reporter"),
            (gateway as JourneyProviderDescriptor).requests.map(Request<*, *>::toString),
        )
        assertEquals(
            listOf("step name", "attempt"),
            (retried as AttemptProviderDescriptor).requests.map(Request<*, *>::toString),
        )
    }

    @ParameterizedTest
    @FieldSource("shops")
    fun `a provider of journey scope supplies what its function returns`(
        shop: String,
        provided: String?,
    ) {
        val gateway = providers().providers.first() as JourneyProviderDescriptor
        val (journey, reporter) = gateway.requests
        val events = mutableListOf<String>()
        val got =
            Answers<JourneyProviderKind<Sync>>(
                mapOf(journey to JourneyId.of("7"), reporter to Recording(events))
            )
        val call = gateway.call as SyncProvider<Orders, JourneyProviderKind<Sync>, *>
        assertEquals(provided, takenBy(call.provide(Orders(shop), got)))
        assertEquals(listOf("info"), events)
    }

    @Test
    fun `a provider of attempt scope receives the step's name and the attempt`() {
        val retried = providers().providers.last() as AttemptProviderDescriptor
        val (step, attempt) = retried.requests
        val got =
            Answers<AttemptProviderKind<Sync>>(
                mapOf(step to StepName.of("charge"), attempt to Attempt.of(2))
            )
        val call = retried.call as SyncProvider<Orders, AttemptProviderKind<Sync>, *>
        assertEquals("charge-2-40", takenBy(call.provide(Orders("corner"), got)))
    }

    @Test
    fun `a provider reporter is called once per event and per metadata entry`() {
        val calls = mutableListOf<String>()
        providerReporters(Recording(calls))
        assertEquals(
            listOf("info", "info", "warning", "warning", "error", "error", "gateway"),
            calls,
        )
    }

    /** What the gateway [provided] takes for 40, or `null` when it provided no instance. */
    private fun takenBy(provided: Provided<*>): String? =
        when (provided) {
            is Provided.Instance -> (provided.instance as PaymentGateway).take(40)
            Provided.NoInstance -> null
        }

    private class Recording(private val calls: MutableList<String>) : ProviderReporter {
        override fun info(message: String) {
            calls += "info"
        }

        override fun info(message: String, data: Any?) {
            calls += "info"
        }

        override fun warning(message: String) {
            calls += "warning"
        }

        override fun warning(message: String, data: Any?) {
            calls += "warning"
        }

        override fun error(message: String) {
            calls += "error"
        }

        override fun error(message: String, data: Any?) {
            calls += "error"
        }

        override fun setGlobalMetadata(key: String, value: Any?) {
            calls += key
        }
    }

    companion object {
        @JvmField
        val shops =
            listOf(
                argumentSet("a gateway for a shop with a name", "corner", "corner-7-40"),
                argumentSet("no instance for a shop with no name", "", null),
            )
    }
}
