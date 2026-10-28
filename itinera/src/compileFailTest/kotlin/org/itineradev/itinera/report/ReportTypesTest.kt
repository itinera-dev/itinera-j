package org.itineradev.itinera.report

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val REPORT = "import org.itineradev.itinera.report.*\n"

class ReportTypesTest {
    @ParameterizedTest
    @FieldSource("factories")
    fun `a factory always makes what it is for`(factory: String, made: String) {
        assertDoesNotCompile(
            "$REPORT val factory = $factory { null }",
            "Null cannot be a value of a non-null type '$made'",
        )
    }

    companion object {
        @JvmField
        val factories =
            listOf(
                argumentSet("a reporter factory", "ReporterFactory", "Reporter"),
                argumentSet("a dispatcher factory", "DispatcherFactory", "Dispatcher"),
            )
    }
}
