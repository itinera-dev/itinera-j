package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class RescueCauseTest {
    @ParameterizedTest
    @FieldSource("rescueCauses")
    fun `a rescue's cause prints as the specification writes it`(
        cause: RescueCause,
        written: String,
    ) {
        assertEquals(written, cause.toString())
    }

    companion object {
        @JvmField
        val rescueCauses =
            listOf(
                argumentSet("after a failure", RescueCause.FAILURE, "failure"),
                argumentSet(
                    "after an abnormal termination",
                    RescueCause.ABNORMAL_TERMINATION,
                    "abnormal termination",
                ),
            )
    }
}
