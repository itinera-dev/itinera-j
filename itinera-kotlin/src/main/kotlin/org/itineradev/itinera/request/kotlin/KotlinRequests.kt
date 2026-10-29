package org.itineradev.itinera.request.kotlin

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.FromStepKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.RoleKind
import org.itineradev.itinera.request.ServiceRequest
import org.itineradev.itinera.request.WorkflowDataKind
import org.itineradev.itinera.value.kotlin.typeToken

/**
 * A required input under [key], read as [T] with Kotlin's nullability at every level, as
 * `Requests.input(key, typeToken<T>())` does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedRequests
 */
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

/**
 * Data the step a hook acts on contributed under [key], read as [T] with Kotlin's nullability at
 * every level, as `Requests.fromStep(key, typeToken<T>())` does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedHookRequests
 */
public inline fun <reified T> Requests.fromStep(key: String): Request<FromStepKind, T> =
    fromStep(key, typeToken<T>())

/**
 * Data the step a hook acts on contributed under [key], read as [T], whose answer is `null` when
 * the step contributed nothing under the key, as `Requests.optionalFromStep(key, typeToken<T>())`
 * does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedHookRequests
 */
public inline fun <reified T> Requests.optionalFromStep(key: String): Request<FromStepKind, T?> =
    optionalFromStep(key, typeToken<T>())

/**
 * Data from the workflow under [key], read as [T] from the data bag, as `Requests.fromWorkflow(key,
 * typeToken<T>())` does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedHookRequests
 */
public inline fun <reified T> Requests.fromWorkflow(key: String): Request<WorkflowDataKind, T> =
    fromWorkflow(key, typeToken<T>())

/**
 * Data from the workflow under [key], read as [T] from the data bag, whose answer is `null` when
 * the data bag holds nothing under the key, as `Requests.optionalFromWorkflow(key, typeToken<T>())`
 * does.
 *
 * @throws IllegalArgumentException when no value can be of [T].
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedHookRequests
 */
public inline fun <reified T> Requests.optionalFromWorkflow(
    key: String
): Request<WorkflowDataKind, T?> = optionalFromWorkflow(key, typeToken<T>())

/**
 * The role [R], as `Requests.role(R::class.java)` does.
 *
 * @throws IllegalArgumentException when [R] is not an interface marked `@Role`.
 * @sample org.itineradev.itinera.request.kotlin.samples.reifiedHookRequests
 */
public inline fun <reified R : Any> Requests.role(): Request<RoleKind, R> = role(R::class.java)
