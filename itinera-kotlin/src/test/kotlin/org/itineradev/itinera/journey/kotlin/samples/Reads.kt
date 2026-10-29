package org.itineradev.itinera.journey.kotlin.samples

import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.journey.kotlin.read

fun reads(data: DataBag): Long = data.read<Parcel>("parcel").orNull()?.weight ?: 0L

fun accessReads(access: DataBagAccess): List<String> =
    access.read<List<String>>("notes").orNull().orEmpty()
