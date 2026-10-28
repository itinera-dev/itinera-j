package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.Asked

/**
 * A typed token for something a part of a workflow requests, such as an input, a service or a
 * contributor: made once, usually as a constant, it declares the request to the builder and then
 * reads its answer from [Got], already of type [T]. [Requests] makes them.
 *
 * [K] says which parts may make the request: a part whose [Got] is of a kind that is not a [K]
 * cannot read it, so a request its kind may not make does not compile, such as a step reading a
 * hook's reporter.
 *
 * @param K the kinds of part that may make the request.
 * @param T the type of its answer.
 * @sample org.itineradev.itinera.request.samples.requests
 */
@ExperimentalItineraApi
public sealed class Request<in K, out T>(internal val asked: Asked, internal val answer: Answer) {
    /**
     * What the request asks for, such as `input amount: Long` or `contributor`.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    override fun toString(): String = asked.toString()
}

/** A request that is neither a service's nor another of the public kinds. */
internal class Asking<in K, out T>(asked: Asked, answer: Answer) : Request<K, T>(asked, answer)
