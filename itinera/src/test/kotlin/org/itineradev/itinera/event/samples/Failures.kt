@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.event.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.RescueCause

fun failures(failure: JourneyFailure): String =
    "${failure.cause}: ${failure.reason?.code ?: failure.errorMessage}${decider(failure.decidedBy)}"

fun givenUp(cause: GiveUpCause): String =
    when (cause) {
        GiveUpCause.Failure,
        GiveUpCause.AbnormalTermination,
        GiveUpCause.RetriesExhausted -> "given up after ${cause.cause}"
        is GiveUpCause.FailWorkflow -> "given up: ${cause.reason.code}"
    }

fun rescued(cause: RescueCause): String = "rescued after $cause"
