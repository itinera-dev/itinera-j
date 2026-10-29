package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.reporting
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.GlobalMetadataSet
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.ReservedMetadataKey
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JourneyTest {
    @Test
    fun `an entry a step sets reaches the reporters, then global_metadata_set names its key and the step`() {
        val log = Log()
        val instance = journeyOf(setting("user_id", "u-7")).reporter(log).create()
        ran(instance)
        assertEquals(
            listOf("journey_id" to instance.journeyId.toString(), "user_id" to "u-7"),
            log.metadata,
        )
        val set = log.events.filterIsInstance<GlobalMetadataSet>().single()
        assertEquals(StepSource(charge, Attempt.of(1)), set.source)
        assertEquals("user_id", set.key)
        assertEquals(
            listOf("journey_started", "attempt_started", "global_metadata_set", "step_succeeded"),
            log.kinds.take(4),
        )
    }

    @Test
    fun `an entry is a copy made when it is set`() {
        val log = Log()
        val items = mutableListOf("book")
        val tagging =
            reporting(
                "charge",
                { reporter ->
                    reporter.setGlobalMetadata("items", items)
                    items += "lamp"
                    Outcome.success()
                },
            )
        ran(journeyOf(tagging).reporter(log).create())
        assertEquals(listOf("book"), log.metadata.last().second)
    }

    @Test
    fun `setting the journey ID aborts the journey with reserved metadata key, and no reporter receives it`() {
        val log = Log()
        val instance = journeyOf(setting("journey_id", "forged")).reporter(log).create()
        val result = ran(instance)
        assertEquals(ReservedMetadataKey(charge, ByStep), result.abort?.details)
        assertEquals(listOf("journey_id" to instance.journeyId.toString()), log.metadata)
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    @Test
    fun `an entry that is not a value aborts the journey with not a value under its key`() {
        val log = Log()
        val result = ran(journeyOf(setting("callback", Thread())).reporter(log).create())
        assertEquals(NotAValue.UnderKey(charge, ByStep, "callback", null), result.abort?.details)
        assertEquals(listOf("journey_id"), log.metadata.map(Pair<String, Any?>::first))
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    @Test
    fun `a reporter that fails on an entry aborts the journey with reporter failed`() {
        val log = Log()
        val result =
            ran(journeyOf(setting("user_id", "u-7")).reporter(log).reporter(FailingOnUser).create())
        assertEquals(ReporterFailed(charge, "user refused"), result.abort?.details)
        assertEquals(listOf("journey_started", "attempt_started", "journey_aborted"), log.kinds)
    }

    private companion object {
        val charge: StepName = StepName.of("charge")

        fun setting(key: String, value: Any?) =
            reporting(
                "charge",
                { reporter ->
                    reporter.setGlobalMetadata(key, value)
                    Outcome.success()
                },
            )
    }
}

private object FailingOnUser : Reporter {
    override fun report(event: Event) = Unit

    override fun setGlobalMetadata(key: String, value: Any?) {
        check(key != "user_id") { "user refused" }
    }
}
