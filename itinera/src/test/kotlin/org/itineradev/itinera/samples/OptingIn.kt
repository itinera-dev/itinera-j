package org.itineradev.itinera.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.RescueCause

@OptIn(ExperimentalItineraApi::class) fun optingIn(): RescueCause = RescueCause.FAILURE
