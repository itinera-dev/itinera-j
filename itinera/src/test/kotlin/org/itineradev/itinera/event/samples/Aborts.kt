package org.itineradev.itinera.event.samples

import org.itineradev.itinera.event.JourneyAbort
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.WrongData
import org.itineradev.itinera.step.StepName

fun aborts(abort: JourneyAbort): String {
    val during = abort.step?.let(::during).orEmpty()
    val because = abort.errorMessage?.let(::because).orEmpty()
    return "${abort.reason}$during$because"
}

fun missing(missing: MissingData): String =
    when (missing) {
        is MissingData.Key -> "the key ${missing.key}"
        is MissingData.FailureReason -> "the failure's reason"
        is MissingData.Error -> "the error"
    }

fun wrong(wrong: WrongData): String =
    when (wrong) {
        is WrongData.Key -> "the key ${wrong.key}"
        is WrongData.FailureReason -> "the failure's reason"
    }

private fun during(step: StepName): String = " during $step"

private fun because(message: String): String = ": $message"
