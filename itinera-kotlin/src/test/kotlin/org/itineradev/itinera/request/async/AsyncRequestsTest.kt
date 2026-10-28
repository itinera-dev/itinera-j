@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.async.samples.asyncRequests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncRequestsTest {
    @Test
    fun `an asynchronous step requests the asynchronous reporter`() {
        assertEquals(
            listOf("async step reporter"),
            asyncRequests().build().requests.map(Request<*, *>::toString),
        )
    }
}
