package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Implementation
import org.itineradev.itinera.conformance.model.ProviderModel
import org.itineradev.itinera.conformance.model.RequestedService
import org.itineradev.itinera.conformance.model.ServiceType
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.ServiceRequest
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.workflow.Mode

/** The cases' `labeller`, whose one operation returns its instance's label. */
internal fun interface Labeller {
    fun label(): String
}

/** The cases' `meter`, an unrelated service whose one operation returns an integer reading. */
internal fun interface Meter {
    fun reading(): Long
}

/**
 * A `labeller` a provider made, labelled [labelled], whose operation fails with [failure] when the
 * case says so.
 */
internal sealed class MadeLabeller(val labelled: String, private val failure: String?) : Labeller {
    override fun label(): String = failure?.let(::failing) ?: labelled
}

/** The cases' `first labeller`. */
internal class FirstLabeller(labelled: String, failure: String?) : MadeLabeller(labelled, failure)

/** The cases' `second labeller`. */
internal class SecondLabeller(labelled: String, failure: String?) : MadeLabeller(labelled, failure)

/** The instance of [type] that [provider] makes, labelled [label]. */
internal fun madeBy(provider: ProviderModel, type: ServiceType, label: String): Any =
    when (type) {
        ServiceType.LABELLER -> labellerOf(provider.implementation, label, provider.servicesFail)
        ServiceType.METER -> Meter { 0 }
    }

/** An instance of [implementation] labelled [label], whose operation fails with [failure]. */
internal fun labellerOf(implementation: Implementation, label: String, failure: String?): Labeller =
    when (implementation) {
        Implementation.FIRST -> FirstLabeller(label, failure)
        Implementation.SECOND -> SecondLabeller(label, failure)
    }

/** How a case writes the implementation `service_provided` names. */
internal fun writtenImplementation(implementation: String): String =
    when (implementation) {
        FirstLabeller::class.java.name -> Implementation.FIRST.written
        SecondLabeller::class.java.name -> Implementation.SECOND.written
        else -> implementation
    }

private fun failing(message: String): Nothing = throw IllegalStateException(message)

/** The interface a service of [type] has in the runner. */
internal fun classOf(type: ServiceType): Class<*> =
    when (type) {
        ServiceType.LABELLER -> Labeller::class.java
        ServiceType.METER -> Meter::class.java
    }

/** The request a step or rescue step makes for [service]. */
internal fun requestOf(service: RequestedService): ServiceRequest<*> =
    Requests.service(service.key, classOf(service.type))

/** The provider attached to the steps [provider] serves only, each as often as named. */
internal fun <W, M : Mode, K> ProviderBuilder<W, M, K>.servingOnly(
    provider: ProviderModel
): ProviderBuilder<W, M, K> = apply { provider.steps.forEach(::step) }
