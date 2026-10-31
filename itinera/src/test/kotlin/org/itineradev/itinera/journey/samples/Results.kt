@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.journey.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.JourneyStatus

fun results(result: JourneyResult): String =
    when (val status = result.status) {
        is JourneyStatus.Succeeded -> "${result.journeyId} succeeded with ${status.dataBag.keys}"
        is JourneyStatus.Failed ->
            "${result.journeyId} failed: ${status.failure.cause}, ${status.failure.errorMessage}"
        is JourneyStatus.Aborted ->
            "${result.journeyId} aborted: ${status.abort.reason} at ${status.abort.step}"
    }
