package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ProviderAction
import org.itineradev.itinera.conformance.model.ProviderFailure
import org.itineradev.itinera.conformance.model.ProviderModel
import org.itineradev.itinera.conformance.model.ProviderRequest
import org.itineradev.itinera.conformance.model.Supplies
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Mode

/**
 * What the scripted [provider] does when called, through requests of kind [K]: the witness records
 * what it received, it does what its actions say through its [reporter], and it fails, returns no
 * instance or makes one, as its script says.
 */
internal class Providing<K, R>(
    private val provider: ProviderModel,
    private val witness: Witness,
    private val journeyId: Request<K, JourneyId>,
    private val reporter: Request<K, R>,
    private val requests: Map<ProviderRequest, Request<K, *>>,
) {
    private val made = mutableMapOf<JourneyId, Int>()

    /** Declares what the provider requests on [builder], in the order written. */
    fun <W, M : Mode> declaredOn(builder: ProviderBuilder<W, M, K>): ProviderBuilder<W, M, K> =
        builder.request(journeyId).request(reporter).apply { requests.values.forEach(::request) }

    /**
     * What the provider gives for one call, given [got], making instances of [type], once
     * [performing] its actions with its reporter.
     */
    fun <T : Any> provided(
        got: Got<K>,
        type: Class<T>,
        performing: (List<ProviderAction>, R) -> Unit,
    ): Provided<T> {
        val received = receivedFrom(got)
        performing(provider.actions, got[reporter])
        return provided(got, received, type)
    }

    /** The asynchronous twin of [provided]. */
    suspend fun <T : Any> providedAsync(
        got: Got<K>,
        type: Class<T>,
        performing: suspend (List<ProviderAction>, R) -> Unit,
    ): Provided<T> {
        val received = receivedFrom(got)
        performing(provider.actions, got[reporter])
        return provided(got, received, type)
    }

    private fun receivedFrom(got: Got<K>): Map<ProviderRequest, Any?> =
        requests
            .mapValues { entry -> got[entry.value] }
            .also { received ->
                witness.called(provider.name, received)
            }

    private fun <T : Any> provided(
        got: Got<K>,
        received: Map<ProviderRequest, Any?>,
        type: Class<T>,
    ): Provided<T> {
        provider.failure?.takeIf { failure -> failsOn(failure, received) }?.let(::failed)
        if (provider.returnsNothing) return Provided.noInstance()
        val count = made.merge(got[journeyId], 1, Int::plus) ?: 1
        val label = labelOf(received, count)
        return Provided.Instance(
            type.cast(madeBy(provider, provider.type, label)),
            dataOf(provider),
        )
    }

    private fun labelOf(received: Map<ProviderRequest, Any?>, count: Int): String =
        provider.labelledBy?.let { key -> receivedFor(received, key).toString() }
            ?: "${provider.name} $count"
}

/** Calls of a provider of journey scope scripted as [provider], given its [reporter]. */
internal fun <M : Mode, R> journeyProviding(
    provider: ProviderModel,
    witness: Witness,
    reporter: Request<JourneyProviderKind<M>, R>,
): Providing<JourneyProviderKind<M>, R> =
    Providing(
        provider,
        witness,
        Requests.journeyId(),
        reporter,
        provider.requests.associateWith(::journeyRequestOf),
    )

/** Calls of a provider of attempt scope scripted as [provider], given its [reporter]. */
internal fun <M : Mode, R> attemptProviding(
    provider: ProviderModel,
    witness: Witness,
    reporter: Request<AttemptProviderKind<M>, R>,
): Providing<AttemptProviderKind<M>, R> =
    Providing(
        provider,
        witness,
        Requests.journeyId(),
        reporter,
        provider.requests.associateWith(::attemptRequestOf),
    )

private fun <M : Mode> journeyRequestOf(
    request: ProviderRequest
): Request<JourneyProviderKind<M>, *> =
    when (request) {
        ProviderRequest.JourneyId -> Requests.journeyId()
        is ProviderRequest.FromWorkflow -> fromWorkflowRequestOf(request)
        ProviderRequest.StepName,
        ProviderRequest.Attempt ->
            throw ModelException(
                "a provider of journey scope requesting $request does not compile: its types refuse it"
            )
    }

private fun <M : Mode> attemptRequestOf(
    request: ProviderRequest
): Request<AttemptProviderKind<M>, *> =
    when (request) {
        ProviderRequest.JourneyId -> Requests.journeyId()
        is ProviderRequest.FromWorkflow -> fromWorkflowRequestOf(request)
        ProviderRequest.StepName -> Requests.stepName()
        ProviderRequest.Attempt -> Requests.attempt()
    }

private fun fromWorkflowRequestOf(request: ProviderRequest.FromWorkflow) =
    if (request.optional) {
        Requests.optionalFromWorkflow(request.key, TypeToken.of(request.type.type))
    } else {
        Requests.fromWorkflow(request.key, TypeToken.of(request.type.type))
    }

/** What the provider received for its request of data from the workflow under [key]. */
internal fun receivedFor(received: Map<ProviderRequest, Any?>, key: String): Any? =
    received.filterKeys { request -> isFromWorkflow(request, key) }.values.firstOrNull()

private fun isFromWorkflow(request: ProviderRequest, key: String): Boolean =
    request is ProviderRequest.FromWorkflow && request.key == key

private fun failsOn(failure: ProviderFailure, received: Map<ProviderRequest, Any?>): Boolean =
    failure.key == null || receivedFor(received, failure.key) == failure.received

private fun failed(failure: ProviderFailure): Nothing = throw IllegalStateException(failure.message)

private fun dataOf(provider: ProviderModel): Any? =
    when (val supplies = provider.supplies) {
        Supplies.Nothing -> null
        is Supplies.Data -> supplies.value
        Supplies.NonValue -> NotAValue
    }

/** What is not a value, which the scripted parts give where a case says so. */
internal object NotAValue
