package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class FailureCauseTest {
    @ParameterizedTest
    @FieldSource("causes")
    fun `failure causes print as the specification writes them`(
        cause: FailureCause,
        written: String,
    ) {
        assertEquals(written, cause.toString())
    }

    companion object {
        @JvmField
        val causes =
            listOf(
                argumentSet("failure", FailureCause.FAILURE, "failure"),
                argumentSet(
                    "retries exhausted",
                    FailureCause.RETRIES_EXHAUSTED,
                    "retries exhausted",
                ),
                argumentSet(
                    "abnormal termination",
                    FailureCause.ABNORMAL_TERMINATION,
                    "abnormal termination",
                ),
                argumentSet("FailWorkflow", FailureCause.FAIL_WORKFLOW, "FailWorkflow"),
            )
    }
}
