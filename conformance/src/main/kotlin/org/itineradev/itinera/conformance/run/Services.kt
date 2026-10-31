package org.itineradev.itinera.conformance.run

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
