package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

fun charging(): StepDescriptor<Sync> {
    /** Notes the amount to charge, less any discount, and a note that may be `null`. */
    @Step
    class Price(
        @Input private val amount: Long,
        @Input private val discount: Long?,
        @Input(required = true) private val note: String?,
    ) : ItineraStep {
        fun run(contributor: Contributor, reporter: StepReporter): Outcome {
            reporter.info("pricing", note)
            contributor.contribute("price", amount - (discount ?: 0))
            return succeed()
        }
    }

    /** Prices each order. */
    @Workflow(name = "pricing")
    @WithStep(Price::class, name = "price")
    class Pricing : ItineraWorkflow

    return WorkflowDescriptor.of(Pricing::class.java).steps.single()
}

/** Takes payments: the service a charge is paid through. */
fun interface PaymentGateway {
    fun take(amount: Long): String
}

fun paying(): Violations {
    /** Charges the amount through the payments service the workflow lends it. */
    @Step
    class Charge(@Input private val amount: Long) : ItineraStep {
        fun run(@Service payments: PaymentGateway, contributor: Contributor): Outcome {
            contributor.contribute("payment", payments.take(amount))
            return succeed()
        }
    }

    /** Charges each order, though it provides no payments service yet. */
    @Workflow(name = "charging")
    @WithStep(Charge::class, name = "charge")
    class Charging : ItineraWorkflow

    return try {
        WorkflowDescriptor.of(Charging::class.java)
        error("The workflow provides no payments service, so it is refused.")
    } catch (refused: Violations) {
        refused
    }
}
