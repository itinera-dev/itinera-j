@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.journey.kotlin.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Datum
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.kotlin.output

fun outputs(result: JourneyResult): String {
    /** What an order was charged, with the total under "amount" and a note that may be absent. */
    data class Receipt(val order: String, @Datum(key = "amount") val total: Long, val note: String?)

    val receipt = result.output<Receipt>()
    return "order ${receipt.order} charged ${receipt.total}, ${receipt.note ?: "no note"}"
}
