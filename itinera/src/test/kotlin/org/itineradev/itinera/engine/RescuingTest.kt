@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Renewals.declinedOnce
import org.itineradev.itinera.engine.Renewals.renewing
import org.itineradev.itinera.engine.Renewals.rescuing
import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.RescueCause
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRescued
import org.itineradev.itinera.event.StepRescuing
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class RescuingTest {
    @Test
    fun `a step that fails is rescued, then attempted again`() {
        val log = Log()
        val result = ran(journeyOf(declinedOnce().rescue(renewing())).reporter(log).create())
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "step_failed",
                "step_rescuing",
                "attempt_started",
                "step_succeeded",
                "step_rescued",
                "attempt_started",
                "step_succeeded",
                "journey_succeeded",
            ),
            log.kinds,
        )
        assertEquals(
            StepRescuing(
                4,
                Journeys.clock.instant(),
                result.journeyId,
                log.events[3].workflow,
                charge,
                Attempt.of(1),
                RescueCause.FAILURE,
                rescue,
            ),
            log.events[3],
        )
        assertEquals(rescue, (log.events[6] as StepRescued).rescue)
        assertTrue(result.isSucceeded)
    }

    @Test
    fun `the rescue step's contributions are committed before step_rescued`() {
        val log = Log()
        val renewed = rescuing { contributor ->
            contributor.contribute("card", "renewed")
            RescueOutcome.success()
        }
        ran(journeyOf(declinedOnce().rescue(renewed)).reporter(log).create())
        assertEquals(
            listOf("step_succeeded", "contribution_committed", "step_rescued"),
            log.kinds.subList(5, 8),
        )
        assertEquals(
            StepSource(rescue, Attempt.of(1)),
            log.events.filterIsInstance<ContributionCommitted>().single().source,
        )
    }

    @Test
    fun `a rescue step that fails is given up, and fails the journey with its reason`() {
        val log = Log()
        val refused = rescuing { RescueOutcome.failure(stolen) }
        val result = ran(journeyOf(declinedOnce().rescue(refused)).reporter(log).create())
        assertEquals(
            listOf("attempt_started", "step_failed", "step_given_up", "journey_failed"),
            log.kinds.subList(4, 8),
        )
        assertEquals(
            GiveUpCause.Failure,
            log.events.filterIsInstance<StepGivenUp>().single().cause,
        )
        assertEquals(rescue, log.events.filterIsInstance<StepGivenUp>().single().step)
        assertEquals(
            JourneyFailure.Failure(stolen),
            log.events.filterIsInstance<JourneyFailed>().single().failure,
        )
        assertEquals(stolen, result.failure?.reason)
    }

    @Test
    fun `a rescue step that ends in an abnormal termination fails the journey with its error`() {
        val down = IllegalStateException("refresh down")
        val result = ran(journeyOf(declinedOnce().rescue(rescuing { throw down })).create())
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
        assertEquals(down, result.failure?.error)
    }

    @ParameterizedTest
    @FieldSource("refused")
    fun `a reporter failing while a step is rescued names the step the journey is on`(
        refuses: (Event) -> Boolean,
        named: StepName,
    ) {
        val result =
            ran(journeyOf(declinedOnce().rescue(renewing())).reporter(Refusing(refuses)).create())
        assertEquals(ReporterFailed(named, "refused"), result.abort?.details)
    }

    /** A reporter that throws on every event that [refuses]. */
    private class Refusing(private val refuses: (Event) -> Boolean) : Reporter {
        override fun report(event: Event) {
            check(!refuses(event)) { "refused" }
        }

        override fun setGlobalMetadata(key: String, value: Any?) = Unit
    }

    companion object {
        private val charge = StepName.of("charge")
        private val rescue = StepName.of("charge_rescue")
        private val stolen = Reason("stolen", "The card was stolen.")

        @JvmField
        val refused =
            listOf(
                argumentSet("on step_rescuing", ::isRescuing, charge),
                argumentSet("on the rescue step's success", ::isRescueSucceeded, rescue),
                argumentSet("on step_rescued", ::isRescued, charge),
            )

        private fun isRescuing(event: Event): Boolean = event is StepRescuing

        private fun isRescued(event: Event): Boolean = event is StepRescued

        private fun isRescueSucceeded(event: Event): Boolean =
            event is StepSucceeded && event.step == rescue
    }
}
