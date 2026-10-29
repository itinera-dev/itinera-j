package com.example.orders

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.kotlin.output
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow

@Step
class Charge(@Input private val amount: Long) : ItineraStep {
    fun run(contributor: Contributor): Outcome {
        if (amount <= 0) return fail("nothing to charge")
        contributor.contribute("receipt", "charged $amount")
        return succeed()
    }
}

@Step
class Ship(@Input private val receipt: String) : ItineraStep {
    fun run(contributor: Contributor): Outcome {
        contributor.contribute("shipped", "after $receipt")
        return succeed()
    }
}

@Workflow(name = "orders")
@WithStep(Charge::class, name = "charge", retryBudget = 2)
@WithStep(Ship::class, name = "ship")
class Orders : ItineraWorkflow

data class Shipment(val shipped: String)

fun main() {
    val executor = LocalExecutor()

    val paid = executor.run(Orders().instance().data("amount", 40L).create())
    check(paid.isSucceeded)
    check(paid.output<Shipment>() == Shipment("after charged 40"))

    val free = executor.run(Orders().instance().data("amount", 0L).create())
    check(free.isFailed)
    check(free.failure?.cause == FailureCause.RETRIES_EXHAUSTED)
    check(free.failure?.reason?.code == "nothing to charge")
}
