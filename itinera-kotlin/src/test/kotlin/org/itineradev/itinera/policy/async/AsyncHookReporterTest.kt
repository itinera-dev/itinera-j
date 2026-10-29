package org.itineradev.itinera.policy.async

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.policy.async.samples.asyncHookReporters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncHookReporterTest {
    @Test
    fun `an asynchronous hook reporter is called once per event and per metadata entry`() =
        runBlocking<Unit> {
            val calls = mutableListOf<String>()
            asyncHookReporters(Recording(calls))
            assertEquals(
                listOf("info", "info", "warning", "warning", "error", "error", "audited"),
                calls,
            )
        }

    private class Recording(private val calls: MutableList<String>) : AsyncHookReporter {
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
