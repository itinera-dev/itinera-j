package org.itineradev.itinera.step

import org.itineradev.itinera.step.samples.reporters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StepReporterTest {
    @Test
    fun `a step reporter is called once per event and per metadata entry`() {
        val calls = mutableListOf<String>()
        reporters(Recording(calls))
        assertEquals(
            listOf("info", "info", "warning", "warning", "error", "error", "tenant"),
            calls,
        )
    }

    private class Recording(private val calls: MutableList<String>) : StepReporter {
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
