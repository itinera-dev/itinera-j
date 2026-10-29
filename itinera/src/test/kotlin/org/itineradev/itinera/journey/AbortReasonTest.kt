package org.itineradev.itinera.journey

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AbortReasonTest {
    @ParameterizedTest
    @FieldSource("reasons")
    fun `abort reasons print as the specification writes them`(
        reason: AbortReason,
        written: String,
    ) {
        assertEquals(written, reason.toString())
    }

    companion object {
        @JvmField
        val reasons =
            listOf(
                    "step could not be built" to AbortReason.STEP_COULD_NOT_BE_BUILT,
                    "policy could not be built" to AbortReason.POLICY_COULD_NOT_BE_BUILT,
                    "required data missing" to AbortReason.REQUIRED_DATA_MISSING,
                    "wrong type" to AbortReason.WRONG_TYPE,
                    "invalid lifecycle" to AbortReason.INVALID_LIFECYCLE,
                    "not a value" to AbortReason.NOT_A_VALUE,
                    "hook failed" to AbortReason.HOOK_FAILED,
                    "reporter failed" to AbortReason.REPORTER_FAILED,
                    "rescue skipped" to AbortReason.RESCUE_SKIPPED,
                    "reserved metadata key" to AbortReason.RESERVED_METADATA_KEY,
                    "unexpected result" to AbortReason.UNEXPECTED_RESULT,
                )
                .map(::named)

        private fun named(case: Pair<String, AbortReason>) =
            argumentSet(case.first, case.second, case.first)
    }
}
