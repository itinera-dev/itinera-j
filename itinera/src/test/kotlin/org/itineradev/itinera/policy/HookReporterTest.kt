@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.samples.hookReporters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HookReporterTest {
    @Test
    fun `a hook reporter is called once per event and per metadata entry`() {
        val calls = mutableListOf<String>()
        hookReporters(Recording(calls))
        assertEquals(
            listOf("info", "info", "warning", "warning", "error", "error", "audited"),
            calls,
        )
    }

    private class Recording(private val calls: MutableList<String>) : HookReporter {
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
}
