package org.itineradev.itinera.samples

import org.itineradev.itinera.ExperimentalItineraApi

@ExperimentalItineraApi fun notClaimedYet(api: String): String = "$api is not claimed yet"

@OptIn(ExperimentalItineraApi::class) fun optingIn(): String = notClaimedYet("this API")
