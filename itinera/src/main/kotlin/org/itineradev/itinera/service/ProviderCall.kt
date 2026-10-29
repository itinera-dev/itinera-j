package org.itineradev.itinera.service

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync

/**
 * How a service provider of a workflow of mode [M] is called: [SyncProvider] in a synchronous
 * workflow, and `AsyncProvider` from `itinera-kotlin` in an asynchronous one. A provider descriptor
 * holds the call of its own mode, so a provider of the other mode cannot join a workflow; the
 * executor checks it again before the journey.
 *
 * @param M the mode of the workflows the provider can join.
 * @param W the class of the workflow object it is called with.
 * @param K the kind of the provider, which says what it may request.
 * @param T the service's type.
 * @sample org.itineradev.itinera.service.samples.providers
 */
public interface ProviderCall<M : Mode, in W, K, out T : Any>

/**
 * A synchronous service provider, called with the workflow object and the answers to its requests
 * in `got`. What it throws fails the step's building.
 *
 * @param W the class of the workflow object it is called with.
 * @param K the kind of the provider, which says what it may request.
 * @param T the service's type.
 * @sample org.itineradev.itinera.service.samples.providers
 */
public fun interface SyncProvider<in W, K, out T : Any> : ProviderCall<Sync, W, K, T> {
    /**
     * Provides an instance of the service, or [Provided.NoInstance].
     *
     * @sample org.itineradev.itinera.service.samples.providers
     */
    public fun provide(workflow: W, got: Got<K>): Provided<T>
}
