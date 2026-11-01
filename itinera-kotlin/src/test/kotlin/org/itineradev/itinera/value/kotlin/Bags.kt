package org.itineradev.itinera.value.kotlin

import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.engine.Values
import org.itineradev.itinera.journey.DataBag

fun bagOf(key: String, value: Any?): DataBag = bagWith(key to value)

fun bagWith(vararg entries: Pair<String, Any?>): DataBag =
    DataBags.of(entries.associate(::captured))

private fun captured(entry: Pair<String, Any?>): Pair<String, Captured> =
    entry.first to captured(entry.second)

fun captured(value: Any?): Captured = Values.capture(value) as Captured
