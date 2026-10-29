package org.itineradev.itinera.step.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.step.async.samples.asyncReporters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncStepReporterTest {
    @Test
    fun `an asynchronous step reporter is called once per event and per metadata entry`() =
        runBlocking<Unit> {
            val calls = mutableListOf<String>()
            asyncReporters(Recording(calls))
            assertEquals(
                listOf("info", "info", "warning", "warning", "error", "error", "warehouse"),
                calls,
            )
        }

    private class Recording(private val calls: MutableList<String>) : AsyncStepReporter {
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
