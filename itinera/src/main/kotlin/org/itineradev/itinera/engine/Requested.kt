package org.itineradev.itinera.engine

import org.itineradev.itinera.request.Asking
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request

/**
 * A request asking [asked], whose answer [answer] gives.
 *
 * Part of itinera's engine, not of its API.
 */
public fun <K, T> requestOf(asked: Asked, answer: Answer): Request<K, T> = Asking(asked, answer)

/** What [request] asks for. */
internal fun askedBy(request: Request<*, *>): Asked = request.asked

/**
 * What a part is given until itinera resolves requests: every read throws, so only a part that
 * reads nothing can be built.
 */
internal class Unresolved<K> : Got<K> {
    override fun <T> get(request: Request<K, T>): T =
        throw IllegalStateException("This version of itinera does not resolve $request yet.")
}

/**
 * A request asking what [request] asks, though only a value that is not `null` is of its type,
 * whose answer [answer] gives: how `itinera-java` reads into an `Optional`, which cannot hold
 * `null`, so a stored `null` is `wrong type`.
 *
 * Part of itinera's engine, not of its API.
 */
public fun <K, T> excludingNull(request: Request<K, *>, answer: Answer): Request<K, T> =
    Asking(excludingNull(request.asked), answer)

private fun excludingNull(asked: Asked): Asked =
    when (asked) {
        is InputAsked -> asked.copy(type = excludingNull(asked.type))
        is ServiceAsked,
        is Handle -> asked
    }

/** [type], though `null` is not of it at the top level. */
internal fun excludingNull(type: ValueType): ValueType =
    when (type) {
        is ClassType -> type.copy(nullable = false)
        is VariableType -> type.copy(nullness = Nullness.NON_NULL)
        AnyValueType -> ClassType(Any::class.java, emptyList(), nullable = false)
    }
