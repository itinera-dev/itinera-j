package org.itineradev.itinera.request.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test

class RequestModeTypesTest {
    @Test
    fun `a synchronous step cannot read the asynchronous step reporter`() {
        assertDoesNotCompile(
            "$MODES fun read(got: Got<StepKind<Sync>>) = got[Requests.asyncStepReporter()]",
            "Argument type mismatch",
        )
    }

    @Test
    fun `an asynchronous step cannot read the synchronous step reporter`() {
        assertDoesNotCompile(
            "$MODES fun read(got: Got<StepKind<Async>>) = got[Requests.stepReporter()]",
            "Argument type mismatch",
        )
    }

    @Test
    fun `a synchronous step cannot declare the asynchronous step reporter`() {
        assertDoesNotCompile(
            "$MODES val ship = StepDescriptor.builder(\"ship\") { got -> error(\"never built\") }" +
                ".request(Requests.asyncStepReporter())",
            "Argument type mismatch",
        )
    }
}
