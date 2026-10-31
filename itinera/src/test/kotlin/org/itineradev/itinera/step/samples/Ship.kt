@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.step.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun

/** Ships the order. */
@Step
class Ship : ItineraStep {
    fun run(): Outcome = succeed()
}

/** Charges the amount. A charge of nothing is declined. */
@Step
class Charge(@Input private val amount: Long) : ItineraStep {
    fun run(): Outcome =
        if (amount <= 0) fail(Reason("declined", "Nothing to charge.", mapOf("amount" to amount)))
        else succeed()
}

/** Takes the discount off, when the order has one. */
@Step
class Discount(@Input private val discount: Long?) : ItineraStep {
    fun run(): Outcome = if (discount == null) skipBecause("no discount") else succeed()
}

fun outcomes(): List<Outcome> =
    listOf(
        Outcome.success(),
        Outcome.failure(Reason("declined", "The card was declined.")),
        Outcome.failure("declined"),
        Outcome.skipped(),
        Outcome.skipped(Reason("no discount", "The order has no discount.")),
        Outcome.skipped("no discount"),
    )

fun charging(): Outcome = Charge(0).run()

fun discounting(): Outcome = Discount(null).run()

fun factories(): SyncStepFactory = SyncStepFactory {
    val ship = Ship()
    SyncStepRun(ship::run)
}
