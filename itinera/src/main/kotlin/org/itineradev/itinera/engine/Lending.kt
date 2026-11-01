package org.itineradev.itinera.engine

import java.util.IdentityHashMap
import org.itineradev.itinera.event.ByProvider
import org.itineradev.itinera.event.ByProviderForKey
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.ProviderSource
import org.itineradev.itinera.event.ServiceProvided
import org.itineradev.itinera.event.ServiceProviderFailed
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.AttemptProviderDescriptor
import org.itineradev.itinera.service.JourneyProviderDescriptor
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode

/**
 * Lends the services the steps of [instance]'s journey request: an instance of journey scope, made
 * the first time a step its provider serves requests it, to every later attempt its provider
 * serves, and an instance of attempt scope to its attempt only.
 */
internal class Lending<M : Mode>(
    private val journey: Journey,
    private val parts: Parts<M>,
    private val instance: WorkflowInstance<M>,
) {
    private val kept = IdentityHashMap<ProviderDescriptor<*, M>, Any>()

    /**
     * The instance of [service] lent to [step]'s [attempt].
     *
     * @throws JourneyAbortSignal when its provider fails, returns no instance, supplies data that
     *   is not a value or returns `null`, and when resolving what it requests aborts the journey.
     */
    @JvmSynthetic
    suspend fun of(step: StepName, attempt: Attempt, service: ServiceAsked): Any {
        val provider =
            servingOf(step, service.key, instance.descriptor.providers)
                ?: throw AssertionError("Admission let in ${service.key} of $step, served by none.")
        return when (provider) {
            is JourneyProviderDescriptor ->
                kept[provider]
                    ?: Providing(provider, step, attempt).made().also { made ->
                        kept[provider] = made
                    }
            is AttemptProviderDescriptor -> Providing(provider, step, attempt).made()
        }
    }

    /** Calling [provider] while [step]'s [attempt] is built. */
    private inner class Providing(
        private val provider: ProviderDescriptor<*, M>,
        private val step: StepName,
        private val attempt: Attempt,
    ) {
        private val call =
            Call(
                RequestedBy(
                    step,
                    ByProvider(provider.name),
                    ProviderSource(provider.name, step, attempt),
                ),
                knownBy(provider),
                emptyMap(),
            )

        /**
         * Calls the provider, once what it requests is resolved, and gives the instance it made.
         *
         * @throws JourneyAbortSignal when it fails, returns no instance, supplies data that is not
         *   a value or returns `null`, and when resolving what it requests aborts the journey.
         */
        @JvmSynthetic
        suspend fun made(): Any {
            val requests = requestsOf(provider)
            val got = Answered<Nothing>(requests, journey.resolved(requests, call))
            val returned = parts.calling { parts.provide(callOf(provider), instance.workflow, got) }
            return lent(returned.getOrElse { thrown -> throw failed(thrown) })
        }

        private fun knownBy(provider: ProviderDescriptor<*, M>): Map<Known, Any> =
            when (provider) {
                is JourneyProviderDescriptor -> mapOf(Known.JOURNEY_ID to instance.journeyId)
                is AttemptProviderDescriptor ->
                    mapOf(
                        Known.JOURNEY_ID to instance.journeyId,
                        Known.STEP_NAME to step,
                        Known.ATTEMPT to attempt,
                    )
            }

        private suspend fun lent(provided: Provided<*>?): Any =
            when (provided) {
                null -> throw unexpected()
                Provided.NoInstance -> throw failed(null)
                is Provided.Instance -> lent(provided)
            }

        private suspend fun lent(provided: Provided.Instance<*>): Any {
            val data = provided.data?.let(::captured)
            val implementation = implementationOf(provided.instance, provider)
            journey.emit {
                ServiceProvided(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    step,
                    attempt,
                    provider.key,
                    provider.name,
                    implementation,
                    data?.value,
                )
            }
            return provided.instance
        }

        private fun captured(data: Any): Captured =
            when (val capture = Values.capture(data)) {
                is Captured -> capture
                is NotAValue -> throw notAValue(capture.error)
            }

        private fun notAValue(error: Throwable?): JourneyAbortSignal {
            val source = ByProvider(provider.name)
            val inEvent =
                NotAValueAbort.InEvent(
                    step,
                    source,
                    EventKind.SERVICE_PROVIDED,
                    error?.let(::messageOf),
                )
            return journey.abort(Abort(inEvent, error))
        }

        private suspend fun failed(thrown: Throwable?): JourneyAbortSignal {
            journey.emit {
                ServiceProviderFailed(
                    sequence,
                    timestamp,
                    journeyId,
                    workflow,
                    step,
                    attempt,
                    provider.key,
                    provider.name,
                )
            }
            val message = thrown?.let(::messageOf)
            val failure = StepCouldNotBeBuilt(step, ByProvider(provider.name), message)
            return journey.abort(Abort(failure, thrown))
        }

        private fun unexpected(): JourneyAbortSignal {
            val message = "the service provider returned null, not what it provides"
            val source = ByProviderForKey(provider.name, provider.key)
            return journey.abort(Abort(UnexpectedResult(step, source, message), null))
        }
    }
}

private fun requestsOf(provider: ProviderDescriptor<*, *>): List<Request<*, *>> =
    when (provider) {
        is JourneyProviderDescriptor -> provider.requests
        is AttemptProviderDescriptor -> provider.requests
    }

/**
 * The implementation [made] is: its class's name, or the type [provider] declares when it has no
 * class of its own, as an instance a lambda makes.
 */
private fun implementationOf(made: Any, provider: ProviderDescriptor<*, *>): String =
    if (made.javaClass.isHidden) provider.type.name else made.javaClass.name
