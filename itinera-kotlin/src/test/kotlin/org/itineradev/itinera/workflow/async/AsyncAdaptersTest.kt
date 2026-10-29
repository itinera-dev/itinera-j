package org.itineradev.itinera.workflow.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.itineradev.itinera.workflow.async.samples.asyncAdapters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncAdaptersTest {
    @Test
    fun `an asynchronous input adapter's call is awaited for what it supplies`() = runBlocking {
        val pricing = asyncAdapters()
        val got = Answers<InputAdapterKind>(mapOf(pricing.requests.single() to "amount"))
        val supplied = (pricing.call as AsyncInputAdapter).adapt(AsyncOrders("corner"), got)
        assertEquals(Supplied.Value(60L), supplied)
    }

    @Test
    fun `an asynchronous workflow takes asynchronous input adapters`() {
        val pricing = asyncAdapters()
        val workflow =
            asyncBuilder<AsyncOrders>("orders")
                .step(asyncStep("charge") { error("never built") }.build())
                .inputAdapter(pricing)
                .build()
        assertEquals(listOf(pricing), workflow.inputAdapters)
    }
}
