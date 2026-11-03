package org.itineradev.itinera.conformance

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EnvironmentTest {
    private val complete =
        mapOf(
            "ITINERA_CONFORMANCE_CASES" to "cases",
            "ITINERA_CONFORMANCE_TAGS" to "@proposal-0121 and not @non-value",
            "ITINERA_CONFORMANCE_REPORT" to "reports/cucumber.json",
        )

    @Test
    fun `the environment says what to run and where to report`() {
        assertEquals(
            Environment(
                Path.of("cases"),
                "@proposal-0121 and not @non-value",
                Path.of("reports/cucumber.json"),
            ),
            Environment.read(complete::get),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            ["ITINERA_CONFORMANCE_CASES", "ITINERA_CONFORMANCE_TAGS", "ITINERA_CONFORMANCE_REPORT"]
    )
    fun `a variable that is not set stops the runner`(variable: String) {
        val error =
            assertThrows<EnvironmentException> { Environment.read((complete - variable)::get) }
        assertEquals("$variable is not set", error.message)
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            ["ITINERA_CONFORMANCE_CASES", "ITINERA_CONFORMANCE_TAGS", "ITINERA_CONFORMANCE_REPORT"]
    )
    fun `an empty variable counts as not set`(variable: String) {
        val error =
            assertThrows<EnvironmentException> {
                Environment.read((complete + (variable to ""))::get)
            }
        assertEquals("$variable is not set", error.message)
    }

    @Test
    fun `tags that are not a tag expression stop the runner`() {
        val written = complete + ("ITINERA_CONFORMANCE_TAGS" to "@proposal-0121 and (")
        val error = assertThrows<EnvironmentException> { Environment.read(written::get) }
        assertEquals(
            "ITINERA_CONFORMANCE_TAGS is not a tag expression: @proposal-0121 and (",
            error.message,
        )
    }
}
