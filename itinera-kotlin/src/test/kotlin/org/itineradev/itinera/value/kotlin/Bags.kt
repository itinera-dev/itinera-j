package org.itineradev.itinera.value.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.engine.Values
import org.itineradev.itinera.journey.DataBag

@OptIn(ExperimentalItineraApi::class)
fun bagOf(key: String, value: Any?): DataBag = DataBags.of(mapOf(key to captured(value)))

fun captured(value: Any?): Captured = Values.capture(value) as Captured
