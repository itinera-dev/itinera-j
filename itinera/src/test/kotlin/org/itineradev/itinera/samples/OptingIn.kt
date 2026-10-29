package org.itineradev.itinera.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.service.ProviderName

@OptIn(ExperimentalItineraApi::class) fun optingIn(): ProviderName = ProviderName.of("stock")
