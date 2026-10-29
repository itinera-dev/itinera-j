package org.itineradev.itinera.instance.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.async.samples.asyncInstances
import org.itineradev.itinera.instance.async.samples.asyncPerJourney
import org.itineradev.itinera.instance.async.samples.typedAsyncInstances
import org.itineradev.itinera.report.async.AsyncDebugReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AsyncInstanceBuilderTest {
    @Test
    fun `an instance holds what it was created with`() {
        val instance = asyncInstances()
        assertEquals(mapOf("order" to "42", "price" to 40L, "discount" to 5L), instance.initialData)
        assertEquals(mapOf("tenant" to "acme"), instance.initialGlobalMetadata)
        assertTrue(instance.reporters.single() is AsyncDebugReporter)
    }

    @Test
    fun `initial data given as a data class leaves out a nullable component holding null`() {
        assertEquals(listOf("order", "price"), typedAsyncInstances().initialData.keys.toList())
    }

    @Test
    fun `a reporter factory makes a reporter for the journey's ID`() =
        runBlocking<Unit> {
            val lines = mutableListOf<String>()
            AsyncLocalExecutor().run(asyncPerJourney(lines))
            assertEquals("corner-1: journey_started", lines.first())
        }
}
