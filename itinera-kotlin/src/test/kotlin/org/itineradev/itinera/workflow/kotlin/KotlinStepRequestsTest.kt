package org.itineradev.itinera.workflow.kotlin

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepFactory
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.annotated.samples.PaymentGateway
import org.itineradev.itinera.workflow.annotated.samples.charging
import org.itineradev.itinera.workflow.annotated.samples.paying
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinStepRequestsTest {
    @Test
    fun `a Kotlin step's nullable inputs are optional, unless marked required`() {
        assertEquals(
            listOf(
                "input amount: Long",
                "optional input discount: Long?",
                "input note: String?",
                "contributor",
                "step reporter",
            ),
            charging().requests.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a Kotlin step is built with its inputs and run with its handles`() {
        val price = charging()
        val contributed = mutableListOf<Pair<String, Any?>>()
        val reporter = Recording()
        val factory = price.factory as SyncStepFactory
        factory.build(answers(price, 40L, 5L, "gift", contributor(contributed), reporter)).run()
        assertEquals(listOf("price" to 35L), contributed)
        assertEquals(listOf("pricing" to "gift"), reporter.infos)
    }

    @Test
    fun `an asynchronous Kotlin step is built with its inputs and run with its handles`() =
        runBlocking<Unit> {
            val ship = asyncDescriptorOf(ParcelShipping::class).steps.single()
            val contributed = mutableListOf<Pair<String, Any?>>()
            val reporter = AsyncRecording()
            val factory = ship.factory as AsyncStepFactory
            factory.build(answers(ship, "7", reporter, contributor(contributed))).run()
            assertEquals(listOf("shipped" to "7"), contributed)
            assertEquals(listOf("shipping" to "7"), reporter.infos)
        }

    @Test
    fun `a step requesting a service is refused while no provider serves it`() {
        assertEquals(
            listOf(Violation.ServiceNotProvided(StepName.of("charge"), "payments")),
            paying().violations,
        )
    }

    @ParameterizedTest
    @FieldSource("misdeclared")
    fun `every mistake in what a Kotlin step requests is refused when it is described`(
        describe: () -> Any,
        problem: String,
    ) {
        val refused = assertThrows<Violations> { describe() }
        assertEquals(listOf(problem), refused.violations.map(::problem))
    }

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem

    private fun contributor(contributed: MutableList<Pair<String, Any?>>): Contributor =
        Contributor { key, value ->
            contributed += key to value
        }

    private fun <K> answers(step: StepDescriptor<*>, vararg answers: Any?): Answers<K> =
        Answers(step.requests.zip(answers).toMap())

    private class Recording : StepReporter {
        val infos = mutableListOf<Pair<String, Any?>>()

        override fun info(message: String) {
            infos += message to null
        }

        override fun info(message: String, data: Any?) {
            infos += message to data
        }

        override fun warning(message: String) = Unit

        override fun warning(message: String, data: Any?) = Unit

        override fun error(message: String) = Unit

        override fun error(message: String, data: Any?) = Unit

        override fun setGlobalMetadata(key: String, value: Any?) = Unit
    }

    private class AsyncRecording : AsyncStepReporter {
        val infos = mutableListOf<Pair<String, Any?>>()

        override suspend fun info(message: String) {
            infos += message to null
        }

        override suspend fun info(message: String, data: Any?) {
            infos += message to data
        }

        override suspend fun warning(message: String) = Unit

        override suspend fun warning(message: String, data: Any?) = Unit

        override suspend fun error(message: String) = Unit

        override suspend fun error(message: String, data: Any?) = Unit

        override suspend fun setGlobalMetadata(key: String, value: Any?) = Unit
    }

    companion object {
        @JvmField
        val misdeclared =
            listOf(
                argumentSet(
                    "a service of a type that admits null",
                    describing { WorkflowDescriptor.of(LenientPaying::class.java) },
                    "requests service payments as a type that admits null, though a service is " +
                        "always required",
                ),
                argumentSet(
                    "the asynchronous reporter in a synchronous step",
                    describing { WorkflowDescriptor.of(ReportingAsynchronously::class.java) },
                    "requests AsyncStepReporter, which only an asynchronous step can",
                ),
                argumentSet(
                    "the synchronous reporter in an asynchronous step",
                    describing { asyncDescriptorOf(ReportingSynchronously::class) },
                    "requests StepReporter, which only a synchronous step can",
                ),
                argumentSet(
                    "a local step class that captures a value",
                    describing { WorkflowDescriptor.of(capturing("7")) },
                    "captures what its enclosing code holds, which itinera cannot give: declare " +
                        "the step at the top level, or as a static nested class",
                ),
            )

        private fun describing(describe: () -> Any) = named("describe", describe)
    }
}

/** A workflow whose step class captures [parcel]. */
fun capturing(parcel: String): Class<out ItineraWorkflow> {
    @Step
    class Capturing : ItineraStep {
        fun run(): Outcome {
            check(parcel.isNotEmpty())
            return succeed()
        }
    }

    @WithStep(Capturing::class, name = "pack") class Packing : ItineraWorkflow

    return Packing::class.java
}

@Step
class ShipParcel(@Input private val parcel: String) : ItineraStep {
    suspend fun run(reporter: AsyncStepReporter, contributor: Contributor): Outcome {
        reporter.info("shipping", parcel)
        contributor.contribute("shipped", parcel)
        return succeed()
    }
}

@Workflow(name = "parcels")
@WithStep(ShipParcel::class, name = "ship")
class ParcelShipping : ItineraAsyncWorkflow

@Step
class PayingLeniently : ItineraStep {
    fun run(@Service payments: PaymentGateway?): Outcome {
        payments?.take(40L)
        return succeed()
    }
}

@Workflow(name = "lenient paying")
@WithStep(PayingLeniently::class, name = "charge")
class LenientPaying : ItineraWorkflow

@Step
class AsyncReporting : ItineraStep {
    @Suppress("UnusedParameter") fun run(reporter: AsyncStepReporter): Outcome = succeed()
}

@Workflow(name = "reporting")
@WithStep(AsyncReporting::class, name = "ship")
class ReportingAsynchronously : ItineraWorkflow

@Step
class SyncReporting : ItineraStep {
    suspend fun run(reporter: StepReporter): Outcome {
        reporter.info("shipping")
        return succeed()
    }
}

@Workflow(name = "reporting")
@WithStep(SyncReporting::class, name = "ship")
class ReportingSynchronously : ItineraAsyncWorkflow
