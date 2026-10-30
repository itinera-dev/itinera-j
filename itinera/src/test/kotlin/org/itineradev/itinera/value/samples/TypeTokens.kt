package org.itineradev.itinera.value.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.value.TypeToken

@OptIn(ExperimentalItineraApi::class)
fun typeTokens() {
    val amount = TypeToken.of(Long::class.java)
    val note = TypeToken.ofNullable(String::class.java)
    check(amount.toString() == "Long")
    check(note.toString() == "String?")
    check(amount == TypeToken.of(java.lang.Long.TYPE))
    check(amount.hashCode() == TypeToken.of(java.lang.Long.TYPE).hashCode())
    check(amount != note)
}

@OptIn(ExperimentalItineraApi::class)
fun genericTypeTokens(): TypeToken<List<Long>> {
    val amounts = object : TypeToken<List<Long>>() {}
    check(amounts.toString() == "List<Long?>?")
    return amounts
}
