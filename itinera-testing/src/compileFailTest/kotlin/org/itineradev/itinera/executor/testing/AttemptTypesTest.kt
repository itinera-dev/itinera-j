package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test

class AttemptTypesTest {
    @Test
    fun `a script chooses an attempt by a whole number`() {
        assertDoesNotCompile(
            """
            import org.itineradev.itinera.executor.testing.*

            fun refused(script: Script) = script.step("charge").onAttempt(1.5)
            """,
            "actual type is 'Double', but 'Int' was expected",
        )
    }

    @Test
    fun `a record reads an attempt by a whole number`() {
        assertDoesNotCompile(
            """
            import org.itineradev.itinera.executor.testing.*

            fun refused(record: StepRecord) = record.attempt(1.5)
            """,
            "actual type is 'Double', but 'Int' was expected",
        )
    }
}
