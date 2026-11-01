package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class ContributionsTest {
    @Test
    fun `the last value contributed under a key in one attempt wins`() {
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("receipt", "R-1")
                    contributor.contribute("receipt", "R-2")
                    Outcome.success()
                },
            )
        assertEquals("R-2", receiptIn(ran(journeyOf(charging).create()).status))
    }

    @Test
    fun `a contribution is captured when it is made`() {
        val charging =
            contributing(
                "charge",
                { contributor ->
                    val items = mutableListOf("book")
                    contributor.contribute("items", items)
                    items += "lamp"
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).create())
        val items =
            (result.status as JourneyStatus.Succeeded)
                .dataBag
                .read("items", TypeToken.of(List::class.java))
                .orNull()
        assertEquals(listOf("book"), items)
    }

    @Test
    fun `a contribution that is not a value aborts the journey, naming the step and the key`() {
        val log = Log()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("thread", Thread())
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(
            NotAValue.UnderKey(StepName.of("charge"), ByStep, "thread", null),
            result.abort?.details,
        )
        assertEquals(
            listOf("journey_started", "attempt_started", "journey_aborted"),
            log.kinds,
        )
    }

    private fun receiptIn(status: JourneyStatus): String? =
        (status as JourneyStatus.Succeeded)
            .dataBag
            .read("receipt", TypeToken.of(String::class.java))
            .orNull()
}
