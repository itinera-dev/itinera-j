@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.async.AsyncRescueRun
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.async.asyncRescueStep
import org.itineradev.itinera.workflow.async.asyncStep

private val TOKEN = Requests.optionalFromStep("token", TypeToken.of(String::class.java))
private val CONTRIBUTOR = Requests.contributor()

/** The charge, whose rescue step asks the bank for a new token, which takes a while. */
fun asyncRescues(): StepDescriptor<Async> {
    val renewal = asyncRescueStep { got ->
        val token = got[TOKEN]
        val contributor = got[CONTRIBUTOR]
        AsyncRescueRun {
            delay(1)
            contributor.contribute("token", "${token ?: "none"}, renewed")
            RescueOutcome.success()
        }
    }
        .request(TOKEN)
        .request(CONTRIBUTOR)
        .build()
    return asyncStep("charge") { AsyncStepRun { Outcome.success() } }.rescue(renewal).build()
}
