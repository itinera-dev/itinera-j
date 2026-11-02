package org.itineradev.itinera.request

/**
 * What a part of kind [K] was given for one call: the answer to each request it declared, which
 * [get] reads by the request's own token, already of its type. The executor resolves every request
 * before the call, so a read never waits.
 *
 * Reading a request the part did not declare throws [IllegalStateException].
 *
 * @param K the kind of part it was given to.
 * @sample org.itineradev.itinera.request.samples.requests
 */
public interface Got<K> {
    /**
     * The answer to [request]: for an optional request, `null` when it was answered absent.
     *
     * @throws IllegalStateException when the part did not declare [request].
     * @sample org.itineradev.itinera.request.samples.requests
     */
    public operator fun <T> get(request: Request<K, T>): T
}
