package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.ProviderReporterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.workflow.Mode

/**
 * What reading the service providers of a workflow class takes in one mode: the reporter of that
 * mode a provider requests by its type, and how each provider method is called.
 *
 * Part of itinera's engine, not of its API.
 */
public interface ProviderCalls<M : Mode> {
    /** The reporter of this mode that a provider requests by its type. */
    public val handles: Map<Class<*>, Request<ProviderReporterKind<M>, *>>

    /**
     * Why a provider cannot request the reporter of the other mode, by the reporter's class name,
     * since this mode's artifact may not hold the class.
     */
    public val otherModes: Map<String, String>

    /** How the provider method [provider] is called on the workflow object. */
    public fun <K> of(provider: ProviderMethod<K>): ProviderCall<M, Any?, K, Any>
}

/**
 * A method of a workflow class that is one of its service providers, of kind [K], and what it
 * requests, in the order of its parameters.
 *
 * Part of itinera's engine, not of its API.
 */
public class ProviderMethod<K>
internal constructor(
    /** The method. */
    public val method: Method,
    private val requests: List<Request<K, *>>,
) {
    /** The arguments it is called with, from its answers in [got]. */
    public fun arguments(got: Got<K>): Array<Any?> =
        requests.map { request -> got[request] }.toTypedArray()
}
