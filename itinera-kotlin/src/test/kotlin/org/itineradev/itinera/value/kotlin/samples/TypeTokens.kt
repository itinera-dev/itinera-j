package org.itineradev.itinera.value.kotlin.samples

import org.itineradev.itinera.value.kotlin.typeToken

fun typeTokens() {
    val amounts = typeToken<List<Long>>()
    val notes = typeToken<Map<String, String?>>()
    check(amounts.toString() == "List<Long>")
    check(notes.toString() == "Map<String, String?>")
}
