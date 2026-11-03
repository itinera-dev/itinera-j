package org.itineradev.itinera.step.testing.samples

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.testing.AsyncStepMock
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncStep

/** The shop whose orders the workflows take. */
class Shop

fun endings(): List<Ending> =
    listOf(
        Ending.success(),
        Ending.failure(Reason("declined", "The card was declined.")),
        Ending.failure("declined"),
        Ending.skipped(),
        Ending.skipped(Reason("no discount", "The order has no discount.")),
        Ending.skipped("no discount"),
        Ending.abnormalTermination("the gateway timed out"),
    )

fun outcomes(): Outcome {
    val timedOut =
        try {
            Ending.abnormalTermination("the gateway timed out").outcome()
        } catch (error: RuntimeException) {
            error.message
        }
    check(timedOut == "the gateway timed out")
    return Ending.success().outcome()
}

fun mocks(): JourneyResult {
    val charge = StepMock.playing(Ending.abnormalTermination("timeout"), Ending.success())
    val orders =
        WorkflowDescriptor.builder(Shop::class.java, "orders")
            .step(StepDescriptor.builder("charge", charge).retryBudget(1).build())
            .build()
    val result = LocalExecutor().run(orders.instance(Shop()).create())
    check(result.isSucceeded && charge.builds == 2)
    return result
}

fun failingToBuild(): AbortReason? {
    val charge = StepMock.failingToBuild("no gateway")
    val orders =
        WorkflowDescriptor.builder(Shop::class.java, "orders")
            .step(StepDescriptor.builder("charge", charge).build())
            .build()
    val result = LocalExecutor().run(orders.instance(Shop()).create())
    check(result.abort?.errorMessage == "no gateway")
    return result.abort?.reason
}

suspend fun asyncFailingToBuild(): AbortReason? {
    val charge = AsyncStepMock.failingToBuild("no gateway")
    val orders = asyncBuilder<Shop>("orders").step(asyncStep("charge", charge).build()).build()
    val result = AsyncLocalExecutor().run(orders.instance(Shop()).create())
    check(result.abort?.errorMessage == "no gateway")
    return result.abort?.reason
}

suspend fun asyncMocks(): JourneyResult {
    val charge = AsyncStepMock.playing(Ending.abnormalTermination("timeout"), Ending.success())
    val orders =
        asyncBuilder<Shop>("orders")
            .step(asyncStep("charge", charge).retryBudget(1).build())
            .build()
    val result = AsyncLocalExecutor().run(orders.instance(Shop()).create())
    check(result.isSucceeded && charge.builds == 2)
    return result
}
