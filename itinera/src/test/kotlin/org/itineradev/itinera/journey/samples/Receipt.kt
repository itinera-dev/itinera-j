package org.itineradev.itinera.journey.samples

import org.itineradev.itinera.journey.Datum

/** What an order was charged: the order under its own name, and the total under "amount". */
@JvmRecord data class Receipt(val order: String, @Datum(key = "amount") val total: Long)
