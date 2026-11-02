package org.itineradev.itinera.event

import org.itineradev.itinera.event.samples.kinds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EventKindTest {
    @Test
    fun `the kinds are those the specification names`() {
        assertEquals(specified, EventKind.entries.map(EventKind::toString).toSet())
    }

    @ParameterizedTest
    @EnumSource(EventKind::class)
    fun `every kind reads back from its name`(kind: EventKind) {
        assertEquals(kind, EventKind.parse(kind.toString()))
    }

    @Test
    fun `a name no kind has reads as nothing`() {
        assertNull(EventKind.parse("Step_Failed"))
    }

    @Test
    fun `the sample reads a kind back`() {
        assertTrue(kinds("step_failed"))
    }

    private val specified =
        setOf(
            "journey_started",
            "attempt_started",
            "input_adapter_supplied",
            "input_adapter_failed",
            "optional_input_absent",
            "service_provided",
            "service_provider_failed",
            "global_metadata_set",
            "step_succeeded",
            "step_failed",
            "step_skipped",
            "step_abnormal_termination",
            "hook_called",
            "contribution_committed",
            "contributions_discarded",
            "data_overwritten",
            "journey_aborted",
            "step_retrying",
            "step_rescuing",
            "step_rescued",
            "step_given_up",
            "journey_succeeded",
            "journey_failed",
            "step_info",
            "step_warning",
            "step_error",
            "journey_info",
            "journey_warning",
            "journey_error",
        )
}
