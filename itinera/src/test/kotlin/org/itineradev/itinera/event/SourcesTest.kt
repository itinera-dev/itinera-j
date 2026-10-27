package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Fixtures.closing
import org.itineradev.itinera.event.Fixtures.stepHookSource
import org.itineradev.itinera.event.samples.sources
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

@OptIn(ExperimentalItineraApi::class)
class SourcesTest {
    @ParameterizedTest
    @FieldSource("requests")
    fun `the sample tells who made a request`(source: RequestSource, told: String) {
        assertEquals(told, sources(source))
    }

    companion object {
        @JvmField
        val requests =
            listOf(
                argumentSet("a step hook", stepHookSource, "audit: on step retry on charge"),
                argumentSet("a workflow hook", closing, "audit: on workflow success"),
            )
    }
}
