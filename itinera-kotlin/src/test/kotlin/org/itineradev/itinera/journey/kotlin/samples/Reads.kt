package org.itineradev.itinera.journey.kotlin.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.kotlin.read

@OptIn(ExperimentalItineraApi::class)
fun reads(data: DataBag): Long = data.read<Parcel>("parcel").orNull()?.weight ?: 0L
