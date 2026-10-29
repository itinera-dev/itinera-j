package org.itineradev.itinera.journey.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.value.kotlin.typeToken

/**
 * Reads the journey's output as a data class or record, generics and nullability included, as
 * [JourneyResult.output] does with the token `typeToken<T>()`.
 *
 * @sample org.itineradev.itinera.journey.kotlin.samples.outputs
 */
@ExperimentalItineraApi
public inline fun <reified T> JourneyResult.output(): T = output(typeToken<T>())
