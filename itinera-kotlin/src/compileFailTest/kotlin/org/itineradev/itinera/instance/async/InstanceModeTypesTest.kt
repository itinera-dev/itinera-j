package org.itineradev.itinera.instance.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertCompiles
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class InstanceModeTypesTest {
    @ParameterizedTest
    @FieldSource("otherModes")
    fun `an instance takes only the reporters of its own mode`(
        builder: String,
        method: String,
        refused: String,
        accepted: String,
    ) {
        assertDoesNotCompile(
            "$MODES fun add(instance: $builder, reporting: $refused) = instance.$method(reporting)",
            "'$refused', but '$accepted' was expected",
        )
        assertCompiles(
            "$MODES fun add(instance: $builder, reporting: $accepted) = instance.$method(reporting)"
        )
    }

    companion object {
        @JvmField
        val otherModes =
            listOf(
                argumentSet(
                    "an asynchronous reporter for a synchronous instance",
                    "InstanceBuilder",
                    "reporter",
                    "AsyncReporter",
                    "Reporter",
                ),
                argumentSet(
                    "an asynchronous reporter factory for a synchronous instance",
                    "InstanceBuilder",
                    "reporterFor",
                    "AsyncReporterFactory",
                    "ReporterFactory",
                ),
                argumentSet(
                    "a synchronous reporter for an asynchronous instance",
                    "AsyncInstanceBuilder",
                    "reporter",
                    "Reporter",
                    "AsyncReporter",
                ),
                argumentSet(
                    "a synchronous reporter factory for an asynchronous instance",
                    "AsyncInstanceBuilder",
                    "reporterFor",
                    "ReporterFactory",
                    "AsyncReporterFactory",
                ),
            )
    }
}
