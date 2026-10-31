package org.itineradev.itinera.value.kotlin

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import org.itineradev.itinera.engine.Values
import org.itineradev.itinera.value.TypeToken

/**
 * The token of a Kotlin type, generics included, with Kotlin's nullability at every level:
 * `typeToken<List<Long?>>()` admits a `null` element and `typeToken<List<Long>>()` refuses one.
 *
 * @throws IllegalArgumentException when no value can be of the type, such as an array, a
 *   `LinkedList` or a map whose keys are not strings.
 * @sample org.itineradev.itinera.value.kotlin.samples.typeTokens
 */
public inline fun <reified T> typeToken(): TypeToken<T> = tokenOf(typeOf<T>())

/** The token of a Kotlin type, which [typeToken] inlines a call to. */
@PublishedApi internal fun <T> tokenOf(type: KType): TypeToken<T> = Values.token(valueType(type))
