@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.executor.testing.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.testing.ScriptedRun
import org.itineradev.itinera.executor.testing.scripted
import org.itineradev.itinera.instance.testing.AsyncTestInstanceBuilder
import org.itineradev.itinera.instance.testing.TestInstanceBuilder
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.report.testing.AsyncRecordingReporter
import org.itineradev.itinera.report.testing.Level
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Charges the order, contributing what it charged. */
@Step
class Charge : ItineraStep {
    fun run(contributor: Contributor, reporter: StepReporter): Outcome {
        contributor.contribute("charged", 40L)
        reporter.info("charged")
        return succeed()
    }
}

/** Ships the order once it is charged. */
@Step
class Ship(@Input private val charged: Long) : ItineraStep {
    fun run(): Outcome = if (charged > 0) succeed() else fail("not charged")
}

/** Takes orders: it charges them, retrying twice, then ships them. */
@Workflow(name = "orders")
@WithStep(Charge::class, name = "charge", retryBudget = 2)
@WithStep(Ship::class, name = "ship")
class Orders : ItineraWorkflow

/** Charges the order, contributing what it charged, without blocking. */
@Step
class AsyncCharge : ItineraStep {
    suspend fun run(contributor: Contributor, reporter: AsyncStepReporter): Outcome {
        contributor.contribute("charged", 40L)
        reporter.info("charged")
        return succeed()
    }
}

/** Ships the order once it is charged, without blocking. */
@Step
class AsyncShip(@Input private val charged: Long) : ItineraStep {
    suspend fun run(): Outcome = if (charged > 0) succeed() else fail("not charged")
}

/** Takes orders without blocking: it charges them, retrying twice, then ships them. */
@Workflow(name = "orders")
@WithStep(AsyncCharge::class, name = "charge", retryBudget = 2)
@WithStep(AsyncShip::class, name = "ship")
class AsyncOrders : ItineraAsyncWorkflow

private val LONG = TypeToken.of(Long::class.javaObjectType)

fun scriptedRuns(): ScriptedRun {
    val recorder = RecordingReporter()
    val instance = TestInstanceBuilder.of(Orders()).reporter(recorder).create()
    val run =
        scripted(instance)
            .step("charge")
            .willFail("declined")
            .onRemainingAttempts()
            .willContribute("charged", 35L)
            .willEmit(Level.WARNING, "charged less")
            .willSucceed()
            .andStep("ship")
            .willRunForReal()
            .ensureAllScripted()
            .run()
    check(run.result.isSucceeded)
    check(
        recorder.path ==
            listOf(
                "charge attempt 1 failed: declined",
                "charge retrying",
                "charge attempt 2 succeeded",
                "ship attempt 1 succeeded",
                "journey succeeded",
            )
    )
    check(run.step("charge").attempts.size == 2)
    check(run.step("ship").attempt(1)?.input("charged", LONG)?.orNull() == 35L)
    return run
}

suspend fun asyncScriptedRuns(): ScriptedRun {
    val recorder = AsyncRecordingReporter()
    val instance = AsyncTestInstanceBuilder.of(AsyncOrders()).reporter(recorder).create()
    val run =
        scripted(instance)
            .step("charge")
            .onEveryAttempt()
            .willContribute("charged", 35L)
            .willSetGlobalMetadata("gateway", "test")
            .willSucceed()
            .run()
    check(run.result.isSucceeded)
    check(recorder.globalMetadata["gateway"] == "test")
    check(run.step("ship").attempt(1)?.input("charged", LONG)?.orNull() == 35L)
    return run
}
