package org.itineradev.itinera.journey.samples

import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.value.TypeToken

fun access(access: DataBagAccess): Long =
    when (val discount = access.read("discount", TypeToken.of(Long::class.javaObjectType))) {
        is Read.Present -> discount.value
        Read.Absent,
        Read.OtherType -> 0L
    }
