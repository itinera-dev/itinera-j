package org.itineradev.itinera.step.testing.samples

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.testing.AsyncRescueStepMock
import org.itineradev.itinera.step.testing.AsyncStepMock
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.RescueEnding
import org.itineradev.itinera.step.testing.RescueStepMock
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncRescueStep
import org.itineradev.itinera.workflow.async.asyncStep

fun rescueEndings(): List<RescueEnding> =
    listOf(
        RescueEnding.success(),
        RescueEnding.failure(Reason("stolen", "The card was stolen.")),
        RescueEnding.failure("stolen"),
        RescueEnding.abnormalTermination("the bank timed out"),
    )

fun rescueOutcomes(): RescueOutcome {
    val timedOut =
        try {
            RescueEnding.abnormalTermination("the bank timed out").outcome()
        } catch (error: RuntimeException) {
            error.message
        }
    check(timedOut == "the bank timed out")
    return RescueEnding.success().outcome()
}

fun rescueMocks(): JourneyResult {
    val renew = RescueStepMock.playing(RescueEnding.success())
    val charge = StepMock.playing(Ending.failure("expired"), Ending.success())
    val orders =
        WorkflowDescriptor.builder(Shop::class.java, "orders")
            .step(
                StepDescriptor.builder("charge", charge)
                    .rescue(RescueStepDescriptor.builder(renew).build())
                    .build()
            )
            .build()
    val result = LocalExecutor().run(orders.instance(Shop()).create())
    check(result.isSucceeded && renew.builds == 1)
    return result
}

fun rescueFailingToBuild(): AbortReason? {
    val renew = RescueStepMock.failingToBuild("no bank")
    val orders =
        WorkflowDescriptor.builder(Shop::class.java, "orders")
            .step(
                StepDescriptor.builder("charge", StepMock.playing(Ending.failure("expired")))
                    .rescue(RescueStepDescriptor.builder(renew).build())
                    .build()
            )
            .build()
    val result = LocalExecutor().run(orders.instance(Shop()).create())
    check(result.abort?.errorMessage == "no bank")
    return result.abort?.reason
}

suspend fun asyncRescueMocks(): JourneyResult {
    val renew = AsyncRescueStepMock.playing(RescueEnding.success())
    val charge = AsyncStepMock.playing(Ending.failure("expired"), Ending.success())
    val orders =
        asyncBuilder<Shop>("orders")
            .step(asyncStep("charge", charge).rescue(asyncRescueStep(renew).build()).build())
            .build()
    val result = AsyncLocalExecutor().run(orders.instance(Shop()).create())
    check(result.isSucceeded && renew.builds == 1)
    return result
}

suspend fun asyncRescueFailingToBuild(): AbortReason? {
    val renew = AsyncRescueStepMock.failingToBuild("no bank")
    val charge = AsyncStepMock.playing(Ending.failure("expired"))
    val orders =
        asyncBuilder<Shop>("orders")
            .step(asyncStep("charge", charge).rescue(asyncRescueStep(renew).build()).build())
            .build()
    val result = AsyncLocalExecutor().run(orders.instance(Shop()).create())
    check(result.abort?.errorMessage == "no bank")
    return result.abort?.reason
}
