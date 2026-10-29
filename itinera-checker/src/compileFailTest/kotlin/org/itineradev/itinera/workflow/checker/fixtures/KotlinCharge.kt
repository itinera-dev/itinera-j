package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.checker.JavaParityTwins

/** A step written in Kotlin, which a Java workflow names. */
@Step
class KotlinCharge : ItineraStep {
    fun run(@Service("payments") payments: JavaParityTwins.Payments): Outcome =
        if (payments.take(1).isEmpty()) fail("unpaid") else succeed()
}
