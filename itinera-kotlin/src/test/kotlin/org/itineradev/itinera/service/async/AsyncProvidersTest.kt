package org.itineradev.itinera.service.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.service.AttemptProviderDescriptor
import org.itineradev.itinera.service.JourneyProviderDescriptor
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.async.samples.Payments
import org.itineradev.itinera.service.async.samples.asyncProviderReporters
import org.itineradev.itinera.service.async.samples.asyncProviders
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncStep
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncProvidersTest {
    @Test
    fun `an asynchronous provider of journey scope is awaited for what it provides`() =
        runBlocking {
            val gateway = asyncProviders().first() as JourneyProviderDescriptor
            val events = mutableListOf<String>()
            val got =
                Answers<JourneyProviderKind<Async>>(
                    mapOf(gateway.requests.single() to Recording(events))
                )
            val call = gateway.call as AsyncProvider<AsyncOrders, JourneyProviderKind<Async>, *>
            assertEquals("corner-40", takenBy(call.provide(AsyncOrders("corner"), got)))
            assertEquals(listOf("info"), events)
        }

    @Test
    fun `an asynchronous provider of attempt scope receives the attempt`() = runBlocking {
        val retried = asyncProviders().last() as AttemptProviderDescriptor
        val got =
            Answers<AttemptProviderKind<Async>>(mapOf(retried.requests.single() to Attempt.of(2)))
        val call = retried.call as AsyncProvider<AsyncOrders, AttemptProviderKind<Async>, *>
        assertEquals("2-40", takenBy(call.provide(AsyncOrders("corner"), got)))
    }

    @Test
    fun `an asynchronous workflow takes asynchronous service providers`() {
        val providers = asyncProviders()
        val workflow =
            asyncBuilder<AsyncOrders>("orders")
                .step(
                    asyncStep("charge") {
                            error("never built")
                        }
                        .build()
                )
        providers.forEach(workflow::provider)
        assertEquals(providers, workflow.build().providers)
    }

    @Test
    fun `an asynchronous provider reporter is called once per event and per metadata entry`() =
        runBlocking {
            val calls = mutableListOf<String>()
            asyncProviderReporters(Recording(calls))
            assertEquals(
                listOf("info", "info", "warning", "warning", "error", "error", "gateway"),
                calls,
            )
        }

    private fun takenBy(provided: Provided<*>): String =
        ((provided as Provided.Instance).instance as Payments).take(40)

    private class Recording(private val calls: MutableList<String>) : AsyncProviderReporter {
        override suspend fun info(message: String) {
            calls += "info"
        }

        override suspend fun info(message: String, data: Any?) {
            calls += "info"
        }

        override suspend fun warning(message: String) {
            calls += "warning"
        }

        override suspend fun warning(message: String, data: Any?) {
            calls += "warning"
        }

        override suspend fun error(message: String) {
            calls += "error"
        }

        override suspend fun error(message: String, data: Any?) {
            calls += "error"
        }

        override suspend fun setGlobalMetadata(key: String, value: Any?) {
            calls += key
        }
    }
}
