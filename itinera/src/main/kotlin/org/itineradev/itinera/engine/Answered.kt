package org.itineradev.itinera.engine

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request

/**
 * What a part was given for one call: the answer the executor [resolved] for each request it
 * [declared], in the form each request gives it, such as an `Optional` from `itinera-java`.
 */
internal class Answered<K>(
    private val declared: List<Request<*, *>>,
    private val resolved: Map<Request<*, *>, Any?>,
) : Got<K> {
    override fun <T> get(request: Request<K, T>): T =
        when (request) {
            in resolved -> returned(request.answer.of(resolved[request]))
            in declared ->
                throw IllegalStateException(
                    "This version of itinera does not resolve $request yet."
                )
            else -> throw IllegalStateException("Nothing answers $request, which was not declared.")
        }
}
