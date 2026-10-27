@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.journey.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.value.TypeToken

fun reads(data: DataBag): String =
    when (val amount = data.read("amount", TypeToken.of(Long::class.java))) {
        is Read.Present -> "charging ${amount.value}"
        Read.Absent -> "nothing to charge"
        Read.OtherType -> "the amount is not a Long: was it written 40 instead of 40L?"
    }

fun keys(data: DataBag): String =
    if ("amount" in data) "keys ${data.keys} in $data" else "no amount"
