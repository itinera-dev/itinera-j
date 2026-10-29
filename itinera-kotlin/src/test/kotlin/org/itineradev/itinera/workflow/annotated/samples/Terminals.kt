package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderReporter
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** Prints a receipt for each payment. */
fun interface Receipts {
    fun print(payment: String): String
}

/** The shop's card terminal, lent to one attempt to settle. */
class Terminal(private val shop: String, private val attempt: Attempt) : PaymentGateway {
    override fun take(amount: Long): String = "$amount paid at the $shop terminal, attempt $attempt"
}

/** Settles the amount at the terminal and prints its receipt. */
@Step
class Settle(@Input private val amount: Long) : ItineraStep {
    fun run(
        @Service payments: PaymentGateway,
        @Service receipts: Receipts,
        contributor: Contributor,
    ): Outcome {
        contributor.contribute("receipt", receipts.print(payments.take(amount)))
        return succeed()
    }
}

/** A shop that settles each order at its card terminal. */
@Workflow(name = "terminal orders")
@WithStep(Settle::class, name = "settle")
class Terminals(private val shop: String) : ItineraWorkflow {
    /** Lends each attempt to settle the shop's terminal, or none for a shop with no name. */
    @Provider(perAttempt = true, steps = ["settle"])
    fun payments(attempt: Attempt, reporter: ProviderReporter): Provided<PaymentGateway> {
        reporter.info("lending the terminal")
        return if (shop.isEmpty()) noInstance() else provide(Terminal(shop, attempt), shop)
    }

    /** Lends every step of the journey the shop's receipt printer. */
    @Provider(key = "receipts", name = "printer")
    fun printing(journey: JourneyId): Provided<Receipts> = provide(Receipts { "$journey: $it" })
}

fun providing(): List<ProviderDescriptor<Terminals, Sync>> =
    WorkflowDescriptor.of(Terminals::class.java).providers

fun misscoped(): Violations {
    /** A shop whose journey-long printer asks for the attempt, which it is never given. */
    @Workflow(name = "misscoped")
    @WithStep(Settle::class, name = "settle")
    class Misscoped : ItineraWorkflow {
        @Provider
        fun receipts(attempt: Attempt): Provided<Receipts> = provide(Receipts { "$attempt" })
    }

    return try {
        WorkflowDescriptor.of(Misscoped::class.java)
        error("The printer of journey scope asks for the attempt, so the workflow is refused.")
    } catch (refused: Violations) {
        refused
    }
}
