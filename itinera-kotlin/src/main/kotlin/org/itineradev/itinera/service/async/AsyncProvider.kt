package org.itineradev.itinera.service.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.workflow.Async

/**
 * An asynchronous service provider, called with the workflow object and the answers to its requests
 * in `got`, and awaited. What it throws fails the step's building.
 *
 * @param W the class of the workflow object it is called with.
 * @param K the kind of the provider, which says what it may request.
 * @param T the service's type.
 * @sample org.itineradev.itinera.service.async.samples.asyncProviders
 */
@ExperimentalItineraApi
public fun interface AsyncProvider<in W, K, out T : Any> : ProviderCall<Async, W, K, T> {
    /**
     * Provides an instance of the service, or [Provided.NoInstance].
     *
     * @sample org.itineradev.itinera.service.async.samples.asyncProviders
     */
    @JvmSynthetic public suspend fun provide(workflow: W, got: Got<K>): Provided<T>
}
