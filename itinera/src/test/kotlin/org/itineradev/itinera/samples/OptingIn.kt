package org.itineradev.itinera.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.value.TypeToken

@OptIn(ExperimentalItineraApi::class)
fun optingIn(): TypeToken<Long> = TypeToken.of(Long::class.java)
