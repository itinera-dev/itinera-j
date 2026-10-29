package org.itineradev.itinera.instance.testing.samples

import org.itineradev.itinera.event.ServiceProvided
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.testing.AsyncTestInstanceBuilder
import org.itineradev.itinera.instance.testing.TestInstanceBuilder
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.report.testing.AsyncRecordingReporter
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Takes payments. */
fun interface Gateway {
    /** Takes [amount], returning the payment's reference. */
    fun take(amount: Long): String
}

/** The real gateway, which a test does not reach. */
class CardGateway : Gateway {
    override fun take(amount: Long): String = error("No test reaches the real gateway.")
}

/** A gateway that accepts every payment, giving it the same reference. */
class AcceptingGateway : Gateway {
    override fun take(amount: Long): String = "accepted"
}

/** Charges the order through the gateway it is lent. */
@Step
class Pay : ItineraStep {
    fun run(@Service gateway: Gateway): Outcome =
        if (gateway.take(40L) == "accepted") succeed() else fail("declined")
}

/** Charges the order through the gateway it is lent, without blocking. */
@Step
class AsyncPay : ItineraStep {
    suspend fun run(@Service gateway: Gateway): Outcome =
        if (gateway.take(40L) == "accepted") succeed() else fail("declined")
}

/** Takes payments through the card gateway. */
@Workflow(name = "payments")
@WithStep(Pay::class, name = "pay")
class Payments : ItineraWorkflow {
    @Provider fun gateway(): Provided<Gateway> = provide(CardGateway())
}

/** Takes payments through the card gateway, without blocking. */
@Workflow(name = "payments")
@WithStep(AsyncPay::class, name = "pay")
class AsyncPayments : ItineraAsyncWorkflow {
    @Provider suspend fun gateway(): Provided<Gateway> = provide(CardGateway())
}

fun fakes(): JourneyResult {
    val recorder = RecordingReporter()
    val instance =
        TestInstanceBuilder.of(Payments())
            .reporter(recorder)
            .fakeProvider("gateway", Gateway::class.java) {
                Provided.Instance(AcceptingGateway(), "fake")
            }
            .create()
    val result = LocalExecutor().run(instance)
    check(result.isSucceeded)
    val provided = recorder.events.filterIsInstance<ServiceProvided>().single()
    check(provided.implementation == AcceptingGateway::class.java.name)
    check(provided.data == "fake")
    return result
}

suspend fun asyncFakes(): JourneyResult {
    val recorder = AsyncRecordingReporter()
    val instance =
        AsyncTestInstanceBuilder.of(AsyncPayments())
            .reporter(recorder)
            .fakeProvider("gateway", Gateway::class.java) { Provided.Instance(AcceptingGateway()) }
            .create()
    val result = AsyncLocalExecutor().run(instance)
    check(result.isSucceeded)
    return result
}
