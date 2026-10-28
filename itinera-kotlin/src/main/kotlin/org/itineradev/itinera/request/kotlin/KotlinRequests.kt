package org.itineradev.itinera.request.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.ServiceRequest
import org.itineradev.itinera.value.kotlin.typeToken

/**
 * A required input under [key], read as [T] with Kotlin's nullability at every level, as
 * `Requests.input(key, typeToken<T>())` does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedRequests
 */
@ExperimentalItineraApi
public inline fun <reified T> Requests.input(key: String): Request<BuildKind, T> =
    input(key, typeToken<T>())

/**
 * An optional input under [key], read as [T] with Kotlin's nullability at every level, whose answer
 * is `null` when the data bag holds nothing under the key, as `Requests.optionalInput(key,
 * typeToken<T>())` does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedRequests
 */
@ExperimentalItineraApi
public inline fun <reified T> Requests.optionalInput(key: String): Request<BuildKind, T?> =
    optionalInput(key, typeToken<T>())

/**
 * A service of type [T] under [key], as `Requests.service(key, T::class.java)` does.
 *
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedRequests
 */
@ExperimentalItineraApi
public inline fun <reified T : Any> Requests.service(key: String): ServiceRequest<T> =
    service(key, T::class.java)
