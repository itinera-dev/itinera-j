package org.itineradev.itinera.service

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode

/**
 * Builds a [ProviderDescriptor] of mode [M], whose kind [K] says what it may request.
 *
 * @param W the class of the workflow object the provider is called with.
 * @param M the mode of the workflows the provider can join.
 * @param K the kind of the provider, of journey or of attempt scope.
 * @sample org.itineradev.itinera.service.samples.providers
 */
@ExperimentalItineraApi
public class ProviderBuilder<W, M : Mode, K>
private constructor(
    private val name: ProviderName,
    private val key: String,
    private val type: Class<*>,
    private val described: (Declared<K>) -> ProviderDescriptor<W, M>,
) {
    private val steps = mutableListOf<StepName>()
    private val requests = mutableListOf<Request<K, *>>()

    /**
     * Attaches the provider to the step named [name], after the steps already attached.
     *
     * @throws IllegalArgumentException when [name] is empty.
     * @sample org.itineradev.itinera.service.samples.providers
     */
    public fun step(name: String): ProviderBuilder<W, M, K> = apply { steps += StepName.of(name) }

    /**
     * Declares a request after those already declared, which the provider then reads.
     *
     * @sample org.itineradev.itinera.service.samples.providers
     */
    public fun request(request: Request<K, *>): ProviderBuilder<W, M, K> = apply {
        requests += request
    }

    /**
     * The descriptor.
     *
     * @sample org.itineradev.itinera.service.samples.providers
     */
    public fun build(): ProviderDescriptor<W, M> =
        described(Declared(name, key, type, steps.toList(), requests.toList()))

    internal companion object {
        fun <W, M : Mode, T : Any> journey(
            name: String,
            key: String,
            type: Class<T>,
            call: ProviderCall<M, W, JourneyProviderKind<M>, T>,
        ): ProviderBuilder<W, M, JourneyProviderKind<M>> =
            ProviderBuilder(ProviderName.of(name), key, type) { declared ->
                JourneyDescribed(declared, call)
            }

        fun <W, M : Mode, T : Any> attempt(
            name: String,
            key: String,
            type: Class<T>,
            call: ProviderCall<M, W, AttemptProviderKind<M>, T>,
        ): ProviderBuilder<W, M, AttemptProviderKind<M>> =
            ProviderBuilder(ProviderName.of(name), key, type) { declared ->
                AttemptDescribed(declared, call)
            }
    }
}

/** What a provider builder declared, whatever the provider's scope. */
internal class Declared<K>(
    val name: ProviderName,
    val key: String,
    val type: Class<*>,
    val steps: List<StepName>,
    val requests: List<Request<K, *>>,
)

private class JourneyDescribed<W, M : Mode>(
    declared: Declared<JourneyProviderKind<M>>,
    override val call: ProviderCall<M, W, JourneyProviderKind<M>, *>,
) : JourneyProviderDescriptor<W, M> {
    override val name: ProviderName = declared.name
    override val key: String = declared.key
    override val type: Class<*> = declared.type
    override val steps: List<StepName> = declared.steps
    override val requests: List<Request<JourneyProviderKind<M>, *>> = declared.requests
}

private class AttemptDescribed<W, M : Mode>(
    declared: Declared<AttemptProviderKind<M>>,
    override val call: ProviderCall<M, W, AttemptProviderKind<M>, *>,
) : AttemptProviderDescriptor<W, M> {
    override val name: ProviderName = declared.name
    override val key: String = declared.key
    override val type: Class<*> = declared.type
    override val steps: List<StepName> = declared.steps
    override val requests: List<Request<AttemptProviderKind<M>, *>> = declared.requests
}
