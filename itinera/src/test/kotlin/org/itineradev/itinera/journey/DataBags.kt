package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.Values

/** Data bags for tests, in Kotlin and in Java, holding what each pair captures. */
@OptIn(ExperimentalItineraApi::class)
object DataBags {
    @JvmStatic
    fun of(vararg entries: Pair<String, Any?>): DataBag =
        org.itineradev.itinera.engine.DataBags.of(entries.associate(::captured))

    @JvmStatic fun of(key: String, value: Any?): DataBag = of(key to value)

    @JvmStatic fun captured(value: Any?): Captured = Values.capture(value) as Captured

    private fun captured(entry: Pair<String, Any?>): Pair<String, Captured> =
        entry.first to captured(entry.second)
}
