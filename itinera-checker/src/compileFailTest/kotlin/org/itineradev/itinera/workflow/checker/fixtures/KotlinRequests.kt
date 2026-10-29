package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** What step classes request, and its mistakes, in Kotlin. */
class KotlinRequests private constructor() {
    interface Payments {
        fun take(amount: Long): String
    }

    @Step
    class AsyncShipping(@Input private val parcel: String) : ItineraStep {
        suspend fun run(reporter: AsyncStepReporter, contributor: Contributor): Outcome {
            reporter.info("shipping", parcel)
            contributor.contribute("shipped", parcel)
            return succeed()
        }
    }

    @WithStep(AsyncShipping::class, name = "ship") class ParcelShipping : ItineraAsyncWorkflow

    @Step
    class PayingLeniently : ItineraStep {
        fun run(@Service payments: Payments?, attempt: Attempt): Outcome {
            payments?.take(attempt.number.toLong())
            return succeed()
        }
    }

    @Step
    class AsyncReporting : ItineraStep {
        fun run(reporter: AsyncStepReporter): Outcome =
            if (reporter.hashCode() > 0) succeed() else fail("none")
    }

    @WithStep(PayingLeniently::class, name = "charge")
    @WithStep(AsyncReporting::class, name = "ship")
    class ReportingAsynchronously : ItineraWorkflow

    @Step
    class SyncReporting : ItineraStep {
        suspend fun run(reporter: StepReporter): Outcome {
            reporter.info("shipping")
            return succeed()
        }
    }

    @WithStep(SyncReporting::class, name = "ship")
    class ReportingSynchronously : ItineraAsyncWorkflow

    @Step
    class Unasked(private val note: String) : ItineraStep {
        fun run(retries: Int): Outcome = if (note.length > retries) succeed() else fail("short")
    }

    @WithStep(Unasked::class, name = "note") class NotRequesting : ItineraWorkflow

    @Step
    class Untyped(
        @Input("anything") private val anything: Any?,
        @Input("required", required = true) private val required: String?,
        @Input("count") private val count: Long?,
    ) : ItineraStep {
        fun run(): Outcome = if (anything == required && count == null) fail("none") else succeed()
    }

    @WithStep(Untyped::class, name = "untyped") class Nullables : ItineraWorkflow
}
