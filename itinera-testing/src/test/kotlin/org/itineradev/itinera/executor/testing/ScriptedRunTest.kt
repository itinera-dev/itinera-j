package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.event.StepWarning
import org.itineradev.itinera.executor.testing.samples.asyncScriptedRuns
import org.itineradev.itinera.executor.testing.samples.scriptedRuns
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.report.testing.complete
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ScriptedRunTest {
    @Test
    fun `a scripted run plays its script and records its steps`() {
        assertTrue(scriptedRuns().result.isSucceeded)
    }

    @Test
    fun `an asynchronous scripted run plays on the asynchronous executor`() {
        assertTrue(complete { asyncScriptedRuns() }.result.isSucceeded)
    }

    @Test
    fun `a scripted ending has the consequences of the outcome it stands for`() {
        val recorder = RecordingReporter()
        chargeScript(recorder)
            .willFail("declined")
            .onAttempt(2)
            .willTerminateAbnormally("timeout")
            .run()
        assertEquals(
            listOf(
                "charge attempt 1 failed: declined",
                "charge retrying",
                "charge attempt 2 terminated abnormally: timeout",
                "charge given up: retries exhausted",
                "journey failed at charge: timeout",
            ),
            recorder.path,
        )
    }

    @Test
    fun `a scripted attempt contributes, emits and sets global metadata as its step would`() {
        val recorder = RecordingReporter()
        val run =
            chargeScript(recorder)
                .willContribute("charged", 35L)
                .willEmit(Level.WARNING, "the gateway is slow", mapOf("seconds" to 3L))
                .willSetGlobalMetadata("gateway", "slow")
                .willSucceed()
                .run()
        val warning = recorder.events.filterIsInstance<StepWarning>().single()
        assertEquals(mapOf("seconds" to 3L), warning.data)
        assertEquals("slow", recorder.globalMetadata["gateway"])
        assertEquals(35L, run.step("ship").attempt(1)?.input("charged", LONG)?.orNull())
    }

    @Test
    fun `an attempt scripted to run for real runs the real step`() {
        val run = chargeScript().onEveryAttempt().willRunForReal().run()
        assertEquals(40L, run.step("ship").attempt(1)?.input("charged", LONG)?.orNull())
    }

    @Test
    fun `a scripted step is still built for every attempt`() {
        val charge = StepMock.playing(Ending.success())
        val shop =
            WorkflowDescriptor.builder(Shop::class.java, "orders")
                .step(StepDescriptor.builder("charge", charge).retryBudget(1).build())
                .build()
        scripted(shop.instance(Shop()).create())
            .step("charge")
            .willFail("declined")
            .onAttempt(2)
            .willSucceed()
            .run()
        assertEquals(2, charge.builds)
    }

    @Test
    fun `a scripted rescue step plays in place of the rescue step's run`() {
        val recorder = RecordingReporter()
        val run =
            scripted(ordered(recorder))
                .step("fix")
                .willContribute("tried", 1L)
                .willFail("broken")
                .onAttempt(2)
                .willSucceed()
                .andStep("fix_rescue")
                .willFail("beyond repair")
                .run()
        assertEquals(
            listOf(
                "fix attempt 1 failed: broken",
                "fix rescuing with fix_rescue",
                "fix_rescue attempt 1 failed: beyond repair",
                "fix_rescue given up: failure",
                "journey failed at fix_rescue: beyond repair",
            ),
            recorder.path.drop(PASSED),
        )
        assertEquals(1L, run.step("fix_rescue").attempt(1)?.input("tried", LONG)?.orNull())
    }

    @Test
    fun `a record holds each attempt handed to its step's construction, in order`() {
        val run = chargeScript().willFail("declined").onAttempt(2).willSucceed().run()
        assertEquals(listOf(1, 2), run.step("charge").attempts.map(::numberOf))
    }

    @Test
    fun `an attempt whose inputs could not be resolved is not recorded`() {
        val run = chargeScript().willSucceed().run()
        assertEquals(AbortReason.REQUIRED_DATA_MISSING, run.result.abort?.reason)
        assertTrue(run.step("ship").attempts.isEmpty())
    }

    @Test
    fun `a step the journey never reached has a record with no attempts`() {
        val run = chargeScript().onEveryAttempt().willFail("declined").run()
        assertTrue(run.step("pack").attempts.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 2])
    fun `reading an attempt the record does not hold reads nothing`(number: Int) {
        val run = chargeScript().willSucceed().run()
        assertNull(run.step("charge").attempt(number))
    }

    @Test
    fun `reading a step the workflow does not have is a mistake, after the journey`() {
        val run = chargeScript().onEveryAttempt().willRunForReal().run()
        val mistake = assertThrows<TestMistake> { run.step("refund") }
        assertEquals(listOf(Mistake.UnknownStep("refund")), mistake.problems)
        assertTrue(run.result.isSucceeded)
    }

    @Test
    fun `a record holds the input an input adapter supplied`() {
        val run = scripted(priced().instance(Shop()).create()).step("ship").willSucceed().run()
        assertEquals(Read.Present(25L), run.step("ship").attempt(1)?.input("charged", LONG))
    }

    @Test
    fun `a record holds nothing for an optional input without a value`() {
        val run = scripted(priced().instance(Shop()).create()).step("weigh").willSucceed().run()
        assertEquals(Read.Absent, run.step("weigh").attempt(1)?.input("weight", LONG))
    }

    private fun chargeScript(reporter: Reporter = RecordingReporter()): NamedStep<StepScript> =
        scripted(ordered(reporter)).step("charge")

    /** A workflow whose `ship` requires `charged`, which `pricing` supplies, after `weigh`. */
    private fun priced(): WorkflowDescriptor<Shop, Sync> =
        WorkflowDescriptor.builder(Shop::class.java, "priced")
            .step(StepDescriptor.builder("weigh", succeeding()).request(WEIGHT).build())
            .step(StepDescriptor.builder("ship", succeeding()).request(CHARGED).build())
            .inputAdapter(
                InputAdapterDescriptor.builder<Shop>("pricing", "ship") { _, _ ->
                        Supplied.Value(25L)
                    }
                    .build()
            )
            .build()

    private fun succeeding(): StepMock = StepMock.playing(Ending.success())

    private fun numberOf(record: AttemptRecord): Int = record.attempt.number

    private companion object {
        /** How many lines the steps before `fix` add to the path, when they succeed. */
        const val PASSED = 3

        val CHARGED = Requests.input("charged", LONG)
        val WEIGHT = Requests.optionalInput("weight", LONG)
    }
}
