package org.itineradev.itinera.instance.samples

import org.itineradev.itinera.journey.Datum

/** An order: its number under its own name, and its amount under "price". */
@JvmRecord data class Order(val order: String, @Datum(key = "price") val amount: Long)
