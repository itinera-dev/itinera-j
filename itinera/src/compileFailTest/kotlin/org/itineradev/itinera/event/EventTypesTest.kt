package org.itineradev.itinera.event

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val JOURNEY =
    """
    import java.time.Instant
    import org.itineradev.itinera.event.*
    import org.itineradev.itinera.journey.JourneyId
    import org.itineradev.itinera.policy.*
    import org.itineradev.itinera.step.*
    import org.itineradev.itinera.workflow.*

    val at = Instant.EPOCH
    val id = JourneyId.of("order-1")
    val orders = WorkflowName.of("orders")
    val pack = StepName.of("pack")
    val first = Attempt.of(1)
    val hook = DecidingHook(PolicyName.of("audit"), StepHook.ON_STEP_FAILURE)
    """

class EventTypesTest {
    @Test
    fun `a contribution comes from a step or a hook, never from an input adapter`() {
        assertDoesNotCompile(
            "$JOURNEY val event = ContributionCommitted(1, at, id, orders, \"k\", " +
                "AdapterSource(AdapterName.of(\"amount\"), pack, first))",
            "'AdapterSource', but 'ContributionSource' was expected",
        )
    }

    @Test
    fun `a journey-level event comes from a hook or a provider, never from a step`() {
        assertDoesNotCompile(
            "$JOURNEY val event = JourneyInfo(1, at, id, orders, StepSource(pack, first), \"m\", null)",
            "'StepSource', but 'EmitterSource' was expected",
        )
    }

    @ParameterizedTest
    @FieldSource("decidedByDefault")
    fun `only the default decides a retry or a rescue`(event: String) {
        assertDoesNotCompile(
            "$JOURNEY val event = $event, decidedBy = hook)",
            "No parameter with name 'decidedBy'",
        )
    }

    companion object {
        @JvmField
        val decidedByDefault =
            listOf(
                argumentSet(
                    "step_retrying",
                    "StepRetrying(1, at, id, orders, pack, first, RetryCause.FAILURE",
                ),
                argumentSet(
                    "step_rescuing",
                    "StepRescuing(1, at, id, orders, pack, first, RescueCause.FAILURE, pack",
                ),
                argumentSet("step_rescued", "StepRescued(1, at, id, orders, pack, first, pack"),
            )
    }
}
