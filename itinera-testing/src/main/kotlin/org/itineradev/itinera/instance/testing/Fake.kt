package org.itineradev.itinera.instance.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.service.Provided

/**
 * A test's own function in place of a synchronous service provider's: called wherever and as often
 * as the provider would be, once its requests are resolved, it returns what a provider returns, and
 * what it throws fails the step's building, as the provider's would. [AsyncFake] is its twin for
 * asynchronous workflows.
 *
 * @param T the service's type.
 * @sample org.itineradev.itinera.instance.testing.samples.fakes
 */
@ExperimentalItineraApi
public fun interface Fake<out T : Any> {
    /**
     * Provides an instance of the service, or [Provided.NoInstance].
     *
     * @sample org.itineradev.itinera.instance.testing.samples.fakes
     */
    public fun provide(): Provided<T>
}

/**
 * A test's own function in place of an asynchronous service provider's, awaited where the provider
 * would be, as [Fake] is called. It needs `itinera-kotlin`.
 *
 * @param T the service's type.
 * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
 */
@ExperimentalItineraApi
public fun interface AsyncFake<out T : Any> {
    /**
     * Provides an instance of the service, or [Provided.NoInstance].
     *
     * @sample org.itineradev.itinera.instance.testing.samples.asyncFakes
     */
    @JvmSynthetic public suspend fun provide(): Provided<T>
}
