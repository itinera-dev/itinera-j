@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.step.StepName

fun names(): String {
    val charge = StepName.of("charge")
    check(charge == StepName.of("charge"))
    check(charge != PolicyName.of("charge") as Any)
    return "journey ${JourneyId.of("order-42")} ran $charge"
}
