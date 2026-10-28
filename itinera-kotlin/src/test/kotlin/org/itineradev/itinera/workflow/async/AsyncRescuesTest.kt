@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.async.AsyncRescueFactory
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.async.samples.AsyncOrders
import org.itineradev.itinera.workflow.async.samples.asyncRescues
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncRescuesTest {
    @Test
    fun `an asynchronous rescue step's run is awaited`() = runBlocking {
        val renewal = asyncRescues().rescue
        val (token, contributor) = renewal?.requests.orEmpty()
        val renewed = mutableListOf<Pair<String, Any?>>()
        val got =
            Answers<RescueKind<Async>>(
                mapOf(
                    token to "old",
                    contributor to Contributor { key, value -> renewed += key to value },
                )
            )
        val run = (renewal?.factory as AsyncRescueFactory).build(got)
        assertEquals(RescueOutcome.Success, run.run())
        assertEquals(listOf("token" to "old, renewed"), renewed)
    }

    @Test
    fun `an asynchronous workflow takes steps with asynchronous rescue steps`() {
        val charge = asyncRescues()
        assertEquals(listOf(charge), asyncBuilder<AsyncOrders>("orders").step(charge).build().steps)
    }
}
