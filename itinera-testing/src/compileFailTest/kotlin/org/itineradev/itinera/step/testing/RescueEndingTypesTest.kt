package org.itineradev.itinera.step.testing

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test

class RescueEndingTypesTest {
    @Test
    fun `a rescue step mock never plays a skip`() {
        assertDoesNotCompile(
            """
            import org.itineradev.itinera.step.testing.*

            val refused = RescueStepMock.playing(Ending.skipped())
            """,
            "actual type is 'Ending', but 'RescueEnding' was expected",
        )
    }
}
