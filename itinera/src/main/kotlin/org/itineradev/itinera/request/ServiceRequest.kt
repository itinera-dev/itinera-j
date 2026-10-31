package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.ServiceAsked

/**
 * The request for a service of type [T] under [key]: a step reads the instance it is lent with it,
 * and the workflow names the service its providers supply by it. A service is always required, and
 * which provider serves it is the workflow's business.
 *
 * @param T the service's type.
 * @sample org.itineradev.itinera.request.samples.requests
 */
@ExperimentalItineraApi
public class ServiceRequest<T : Any>
internal constructor(
    /** The key, unique among the services one step requests. */
    public val key: String,
    /** The service's type, which a provider of [key] supplies exactly. */
    public val type: Class<T>,
) : Request<BuildKind, T>(ServiceAsked(key, type), Answer.ITSELF)
