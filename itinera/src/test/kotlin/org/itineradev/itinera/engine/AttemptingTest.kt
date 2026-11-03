package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.journeyOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Renewals.declinedOnce
import org.itineradev.itinera.engine.Renewals.rescuing
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncRescueFactory
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AttemptingTest {
    private val charge = StepName.of("charge")
    private val rescue = StepName.of("charge_rescue")
    private val aborted = listOf("journey_started", "attempt_started", "journey_aborted")

    @Test
    fun `a step that catches the executor's signal still ends the journey aborted`() {
        val log = Log()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    @Suppress("SwallowedException", "TooGenericExceptionCaught")
                    try {
                        contributor.contribute("thread", Thread())
                    } catch (caught: RuntimeException) {
                        contributor.contribute("caught", caught.toString())
                    }
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(NotAValue.UnderKey(charge, ByStep, "thread", null), result.abort?.details)
        assertEquals(aborted, log.kinds)
    }

    @Test
    fun `a step that lets the executor's signal escape does not terminate abnormally`() {
        val log = Log()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("thread", Thread())
                    Outcome.success()
                },
            )
        ran(journeyOf(charging).reporter(log).create())
        assertEquals(aborted, log.kinds)
    }

    @Test
    fun `a reason whose details are not a value aborts the journey with no outcome fact`() {
        val log = Log()
        val charging =
            contributing("charge", { Outcome.failure(Reason("declined", null, Thread())) })
        val result = ran(journeyOf(charging).reporter(log).create())
        assertEquals(NotAValue.InDetails(charge, ByStep, null), result.abort?.details)
        assertEquals(aborted, log.kinds)
    }

    @Test
    fun `a rescue step whose failure's details are not a value aborts the journey, naming it`() {
        val stolen = rescuing { RescueOutcome.failure(Reason("stolen", null, Thread())) }
        val result = ran(journeyOf(declinedOnce().rescue(stolen)).create())
        assertEquals(NotAValue.InDetails(rescue, ByStep, null), result.abort?.details)
    }

    @Test
    fun `a rescue step that cannot be built aborts the journey, naming it`() {
        val broken = RescueStepDescriptor.builder { throw IllegalStateException("no client") }
        val result = ran(journeyOf(declinedOnce().rescue(broken.build())).create())
        assertEquals(StepCouldNotBeBuilt(rescue, ByStep, "no client"), result.abort?.details)
    }

    @ParameterizedTest
    @FieldSource("nulls")
    fun `a rescue step that gives null is an unexpected result naming it`(
        factory: SyncRescueFactory,
        message: String,
    ) {
        val result =
            ran(
                journeyOf(declinedOnce().rescue(RescueStepDescriptor.builder(factory).build()))
                    .create()
            )
        assertEquals(UnexpectedResult(rescue, ByStep, message), result.abort?.details)
    }

    @Test
    fun `a contributor used after its attempt throws and changes nothing`() {
        var kept: Contributor? = null
        val charging =
            contributing(
                "charge",
                { contributor ->
                    kept = contributor
                    Outcome.success()
                },
            )
        val result = ran(journeyOf(charging).create())
        assertThrows<IllegalStateException> { kept?.contribute("late", "L-1") }
        assertEquals(emptySet<String>(), (result.status as JourneyStatus.Succeeded).dataBag.keys)
    }

    companion object {
        @JvmField
        val nulls =
            listOf(
                argumentSet(
                    "from its factory",
                    SyncRescueFactory { returned(null) },
                    "the rescue step's factory returned null, not a rescue step",
                ),
                argumentSet(
                    "from its run",
                    SyncRescueFactory { SyncRescueRun { returned(null) } },
                    "the rescue step's run returned null, not an outcome",
                ),
            )
    }
}
