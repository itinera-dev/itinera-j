package org.itineradev.itinera.journey.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.value.kotlin.typeToken

/**
 * Reads the value under a key as a Kotlin type, generics and nullability included, as
 * [DataBag.read] does with the token `typeToken<T>()`.
 *
 * @sample org.itineradev.itinera.journey.kotlin.samples.reads
 */
@ExperimentalItineraApi
public inline fun <reified T> DataBag.read(key: String): Read<T> = read(key, typeToken<T>())
