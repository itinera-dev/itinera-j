package org.itineradev.itinera.step.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertCompiles
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test

class AsyncStepTypesTest {
    @Test
    fun `an asynchronous step factory always builds a step`() {
        assertDoesNotCompile(
            "$MODES val pack = asyncStep(\"pack\") { null }",
            "Null cannot be a value of a non-null type 'AsyncStepRun'",
        )
        assertCompiles(
            "$MODES val pack = asyncStep(\"pack\") { AsyncStepRun { Outcome.success() } }"
        )
    }

    @Test
    fun `an asynchronous step's run always gives an outcome`() {
        assertDoesNotCompile(
            "$MODES val run = AsyncStepRun { null }",
            "Null cannot be a value of a non-null type 'Outcome'",
        )
        assertCompiles("$MODES val run = AsyncStepRun { Outcome.success() }")
    }
}
