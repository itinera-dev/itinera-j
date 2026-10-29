package org.itineradev.itinera.journey.samples

import org.itineradev.itinera.journey.Contributor

fun contributors(contributor: Contributor) {
    contributor.contribute("charged", 40L)
    contributor.contribute("receipt", mapOf("amount" to 40L, "currency" to "EUR"))
}
