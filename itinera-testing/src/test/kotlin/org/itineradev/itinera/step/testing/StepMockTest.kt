package org.itineradev.itinera.step.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.report.testing.complete
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.testing.samples.Shop
import org.itineradev.itinera.step.testing.samples.asyncFailingToBuild
import org.itineradev.itinera.step.testing.samples.asyncMocks
import org.itineradev.itinera.step.testing.samples.endings
import org.itineradev.itinera.step.testing.samples.failingToBuild
import org.itineradev.itinera.step.testing.samples.mocks
import org.itineradev.itinera.step.testing.samples.outcomes
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalItineraApi::class)
class StepMockTest {
    @Test
    fun `each attempt ends with the next ending, and every later one with the last`() {
        val reporter = RecordingReporter()
        val charge =
            StepMock.playing(
                Ending.abnormalTermination("first"),
                Ending.abnormalTermination("second"),
            )
        ran(charge, retryBudget = 3, reporter)
        assertEquals(
            listOf(
                "charge attempt 1 terminated abnormally: first",
                "charge retrying",
                "charge attempt 2 terminated abnormally: second",
                "charge retrying",
                "charge attempt 3 terminated abnormally: second",
                "charge retrying",
                "charge attempt 4 terminated abnormally: second",
                "charge given up: retries exhausted",
                "journey failed at charge: second",
            ),
            reporter.path,
        )
    }

    @Test
    fun `an abnormal termination carries the scripted message`() {
        val result = ran(StepMock.playing(Ending.abnormalTermination("timeout")))
        assertEquals(FailureCause.ABNORMAL_TERMINATION, result.failure?.cause)
        assertEquals("timeout", result.failure?.errorMessage)
    }

    @Test
    fun `each build makes a new step and is counted`() {
        val charge = StepMock.playing(Ending.success())
        val first = charge.build()
        assertNotSame(first, charge.build())
        assertEquals(2, charge.builds)
    }

    @Test
    fun `a mock failing to build aborts the journey, and no build is counted`() {
        val charge = StepMock.failingToBuild("no gateway")
        val result = ran(charge)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort?.reason)
        assertEquals("no gateway", result.abort?.errorMessage)
        assertEquals(0, charge.builds)
    }

    @Test
    fun `a mock goes on with its script in the next journey that builds from it`() {
        val reporter = RecordingReporter()
        val charge = StepMock.playing(Ending.abnormalTermination("first"), Ending.success())
        ran(charge)
        ran(charge, reporter = reporter)
        assertEquals(listOf("charge attempt 1 succeeded", "journey succeeded"), reporter.path)
    }

    @Test
    fun `an ending gives the outcome it stands for, or throws what ends the attempt`() {
        assertEquals(Outcome.success(), outcomes())
    }

    @Test
    fun `the samples run their journeys`() {
        assertEquals(
            listOf(Ending.Success, Ending.AbnormalTermination("the gateway timed out")),
            endings(),
        )
        assertTrue(mocks().isSucceeded)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, failingToBuild())
    }

    @Test
    fun `the asynchronous mock plays its script as the synchronous one does`() {
        assertTrue(complete { asyncMocks() }.isSucceeded)
        assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, complete { asyncFailingToBuild() })
    }

    private fun ran(
        charge: StepMock,
        retryBudget: Int = 0,
        reporter: RecordingReporter = RecordingReporter(),
    ): JourneyResult {
        val orders =
            WorkflowDescriptor.builder(Shop::class.java, "orders")
                .step(StepDescriptor.builder("charge", charge).retryBudget(retryBudget).build())
                .build()
        return LocalExecutor().run(orders.instance(Shop()).reporter(reporter).create())
    }
}
