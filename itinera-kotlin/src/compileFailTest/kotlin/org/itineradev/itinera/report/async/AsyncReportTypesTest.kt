package org.itineradev.itinera.report.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertCompiles
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AsyncReportTypesTest {
    @ParameterizedTest
    @FieldSource("factories")
    fun `an asynchronous factory always makes what it is for`(
        factory: String,
        made: String,
        twin: String,
    ) {
        assertDoesNotCompile(
            "$MODES val factory = $factory { null }",
            "Null cannot be a value of a non-null type '$made'",
        )
        assertCompiles("$MODES val factory = $factory { $twin }")
    }

    companion object {
        @JvmField
        val factories =
            listOf(
                argumentSet(
                    "an asynchronous reporter factory",
                    "AsyncReporterFactory",
                    "AsyncReporter",
                    "AsyncReporter { }",
                ),
                argumentSet(
                    "an asynchronous dispatcher factory",
                    "AsyncDispatcherFactory",
                    "AsyncDispatcher",
                    "AsyncDefaultDispatcher()",
                ),
            )
    }
}
