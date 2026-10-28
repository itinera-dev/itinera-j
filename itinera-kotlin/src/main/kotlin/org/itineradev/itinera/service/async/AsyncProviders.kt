package org.itineradev.itinera.service.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Builders
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.workflow.Async

/**
 * Starts the descriptor of an asynchronous provider of journey scope named [name], supplying the
 * service of [key], of [type], and attached to no step until the builder attaches it.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.service.async.samples.asyncProviders
 */
@ExperimentalItineraApi
public fun <W, T : Any> asyncProvider(
    name: String,
    key: String,
    type: Class<T>,
    provider: AsyncProvider<W, JourneyProviderKind<Async>, T>,
): ProviderBuilder<W, Async, JourneyProviderKind<Async>> =
    Builders.provider(name, key, type, provider)

/**
 * Starts the descriptor of an asynchronous provider of attempt scope named [name], supplying the
 * service of [key], of [type], and attached to no step until the builder attaches it.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.service.async.samples.asyncProviders
 */
@ExperimentalItineraApi
public fun <W, T : Any> asyncPerAttemptProvider(
    name: String,
    key: String,
    type: Class<T>,
    provider: AsyncProvider<W, AttemptProviderKind<Async>, T>,
): ProviderBuilder<W, Async, AttemptProviderKind<Async>> =
    Builders.perAttemptProvider(name, key, type, provider)
