package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.report.testing.complete
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.testing.samples.Shop
import org.itineradev.itinera.step.testing.samples.asyncRescueFailingToBuild
import org.itineradev.itinera.step.testing.samples.asyncRescueMocks
import org.itineradev.itinera.step.testing.samples.rescueEndings
import org.itineradev.itinera.step.testing.samples.rescueFailingToBuild
import org.itineradev.itinera.step.testing.samples.rescueMocks
import org.itineradev.itinera.step.testing.samples.rescueOutcomes
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class RescueStepMockTest {
    @Test
    fun `a rescue step mock that succeeds lets its step run again`() {
        val reporter = RecordingReporter()
        val result = ran(RescueStepMock.playing(RescueEnding.success()), reporter)
        assertTrue(result.isSucceeded)
        assertEquals(
            listOf(
                "charge attempt 1 failed: expired",
                "charge rescuing with charge_rescue",
                "charge_rescue attempt 1 succeeded",
                "charge rescued by charge_rescue",
                "charge attempt 2 succeeded",
                "journey succeeded",
            ),
            reporter.path,
        )
    }

    @Test
    fun `a rescue step mock that fails fails the journey for its reason`() {
        val result = ran(RescueStepMock.playing(RescueEnding.failure("stolen")))
        assertEquals(FailureCause.FAILURE, result.failure?.cause)
        assertEquals(Reason("stolen"), result.failure?.reason)
    }

    @Test
    fun `an abnormal termination of a rescue step mock carries the scripted message`() {
        val result = ran(RescueStepMock.playing(RescueEnding.abnormalTermination("timeout")))
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
        assertEquals("timeout", result.failure?.errorMessage)
    }

    @Test
    fun `each build of a rescue step mock makes a new rescue step and is counted`() {
        val renew = RescueStepMock.playing(RescueEnding.success())
        val first = renew.build(Answers(emptyMap()))
        assertNotSame(first, renew.build(Answers(emptyMap())))
        assertEquals(2, renew.builds)
    }

    @Test
    fun `a rescue step mock failing to build aborts the journey, and no build is counted`() {
        val renew = RescueStepMock.failingToBuild("no bank")
        val result = ran(renew)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort?.reason)
        assertEquals("no bank", result.abort?.errorMessage)
        assertEquals(0, renew.builds)
    }

    @Test
    fun `a rescue ending gives the outcome it stands for, or throws what ends the attempt`() {
        assertEquals(RescueOutcome.success(), rescueOutcomes())
        assertEquals(
            RescueOutcome.failure(Reason("stolen", "No.")),
            RescueEnding.failure(Reason("stolen", "No.")).outcome(),
        )
    }

    @Test
    fun `the rescue samples run their journeys`() {
        assertEquals(
            listOf(
                RescueEnding.Success,
                RescueEnding.Failure(Reason("stolen", "The card was stolen.")),
                RescueEnding.Failure(Reason("stolen")),
                RescueEnding.AbnormalTermination("the bank timed out"),
            ),
            rescueEndings(),
        )
        assertTrue(rescueMocks().isSucceeded)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, rescueFailingToBuild())
    }

    @Test
    fun `the asynchronous rescue step mock plays its script as the synchronous one does`() {
        assertTrue(complete { asyncRescueMocks() }.isSucceeded)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, complete { asyncRescueFailingToBuild() })
    }

    private fun ran(
        renew: RescueStepMock,
        reporter: RecordingReporter = RecordingReporter(),
    ): JourneyResult {
        val charge = StepMock.playing(Ending.failure("expired"), Ending.success())
        val orders =
            WorkflowDescriptor.builder(Shop::class.java, "orders")
                .step(
                    StepDescriptor.builder("charge", charge)
                        .rescue(RescueStepDescriptor.builder(renew).build())
                        .build()
                )
                .build()
        return LocalExecutor().run(orders.instance(Shop()).reporter(reporter).create())
    }
}
