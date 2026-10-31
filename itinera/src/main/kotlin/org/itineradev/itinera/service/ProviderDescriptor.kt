package org.itineradev.itinera.service

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync

/**
 * How one service provider of a workflow of mode [M] is declared: its name, unique among the
 * workflow's providers, the key and type of the service it supplies, and the steps it is attached
 * to. Its scope is its type: a [JourneyProviderDescriptor] lends one instance for the journey, and
 * an [AttemptProviderDescriptor] one for each attempt.
 *
 * A provider attached to steps serves those steps, in place of the provider of its key attached to
 * no step, which serves every other step requesting the key.
 *
 * [builder] and [perAttempt] make one for a synchronous workflow, and `asyncProvider` and
 * `asyncPerAttemptProvider` from `itinera-kotlin` for an asynchronous one. Any other implementation
 * of its two kinds runs too, checked by the executor before the journey.
 *
 * @param W the class of the workflow object it is called with.
 * @param M the mode of the workflows the provider can join.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi
public sealed interface ProviderDescriptor<in W, M : Mode> {
    /** The provider's name, unique among the workflow's service providers. */
    public val name: ProviderName

    /** The key of the service it supplies, as the steps it serves request it. */
    public val key: String

    /** The type of the service it supplies, which a step it serves requests exactly. */
    public val type: Class<*>

    /** The steps it is attached to, in the order declared; none when it serves every step. */
    public val steps: List<StepName>

    /** Makes service provider descriptors. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous provider of journey scope named [name], supplying
         * the service of [key], of [type], and attached to no step until the builder attaches it.
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.service.samples.providers
         */
        @JvmStatic
        public fun <W, T : Any> builder(
            name: String,
            key: String,
            type: Class<T>,
            provider: SyncProvider<W, JourneyProviderKind<Sync>, T>,
        ): ProviderBuilder<W, Sync, JourneyProviderKind<Sync>> =
            ProviderBuilder.journey(name, key, type, provider)

        /**
         * Starts the descriptor of a synchronous provider of attempt scope named [name], supplying
         * the service of [key], of [type], and attached to no step until the builder attaches it.
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.service.samples.providers
         */
        @JvmStatic
        public fun <W, T : Any> perAttempt(
            name: String,
            key: String,
            type: Class<T>,
            provider: SyncProvider<W, AttemptProviderKind<Sync>, T>,
        ): ProviderBuilder<W, Sync, AttemptProviderKind<Sync>> =
            ProviderBuilder.attempt(name, key, type, provider)
    }
}

/**
 * A service provider of journey scope: called the first time a step it serves needs its service in
 * a journey, and its instance lent to every later attempt of the steps it serves, until the journey
 * ends. It requests neither the step's name nor the attempt, since it is called once for whichever
 * step needs it first.
 *
 * @param W the class of the workflow object it is called with.
 * @param M the mode of the workflows the provider can join.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi
public interface JourneyProviderDescriptor<in W, M : Mode> : ProviderDescriptor<W, M> {
    /** What it requests, in the order declared, resolved in that order before each call. */
    public val requests: List<Request<JourneyProviderKind<M>, *>>

    /** How it is called. */
    public val call: ProviderCall<M, W, JourneyProviderKind<M>, *>
}

/**
 * A service provider of attempt scope: called for every attempt of every step it serves that needs
 * its service, and its instance lent to that attempt only.
 *
 * @param W the class of the workflow object it is called with.
 * @param M the mode of the workflows the provider can join.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi
public interface AttemptProviderDescriptor<in W, M : Mode> : ProviderDescriptor<W, M> {
    /** What it requests, in the order declared, resolved in that order before each call. */
    public val requests: List<Request<AttemptProviderKind<M>, *>>

    /** How it is called. */
    public val call: ProviderCall<M, W, AttemptProviderKind<M>, *>
}
