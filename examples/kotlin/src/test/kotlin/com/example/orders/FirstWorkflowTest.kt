package com.example.orders

import java.io.File
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.report.testing.RecordingReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirstWorkflowTest {
    @Test fun `the first workflow runs as its checks expect`() = main()

    @Test
    fun `a charge of nothing is attempted three times before the journey fails`() {
        val recording = RecordingReporter()
        LocalExecutor().run(Orders().instance().data("amount", 0L).reporter(recording).create())
        assertEquals(
            listOf(
                "charge attempt 1 failed: nothing to charge",
                "charge retrying",
                "charge attempt 2 failed: nothing to charge",
                "charge retrying",
                "charge attempt 3 failed: nothing to charge",
                "charge given up: retries exhausted",
                "journey failed at charge: nothing to charge",
            ),
            recording.path,
        )
    }

    @Test
    fun `the README names the KSP release the checker is built against`() {
        val readme = File(System.getProperty("example.readme")).readText()
        val ksp = System.getProperty("example.ksp")
        assertTrue(readme.contains("id(\"com.google.devtools.ksp\") version \"$ksp\"")) {
            "The README names a KSP release other than $ksp."
        }
    }

    @Test
    fun `the README shows the Kotlin example as it is`() {
        val readme = File(System.getProperty("example.readme")).readText()
        val shown = Regex("### In Kotlin\n\n```kotlin\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        assertEquals(
            File(System.getProperty("example.source")).readText(),
            shown.find(readme)?.groupValues?.get(1),
        )
    }
}
