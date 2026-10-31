@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.step.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun

/** Ships the order. */
class Ship : ItineraStep {
    fun run(): Outcome = succeed()
}

fun outcomes(): Outcome = Outcome.success()

fun factories(): SyncStepFactory = SyncStepFactory {
    val ship = Ship()
    SyncStepRun(ship::run)
}
