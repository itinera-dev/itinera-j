package org.itineradev.itinera.conformance.model

/** A service type of the cases. */
internal enum class ServiceType(val written: String) {
    LABELLER("labeller"),
    METER("meter");

    companion object {
        private val byName = entries.associateBy(ServiceType::written)

        /** The service type a case names. */
        fun named(written: String): ServiceType =
            byName[written] ?: throw ModelException("$written is not a service type of the cases")
    }
}

/** A service a step or rescue step requests: its key and its type. */
internal data class RequestedService(val key: String, val type: ServiceType)

/** How long an instance a provider makes is lent. */
internal enum class ProviderScope {
    /** One instance for the whole journey. */
    JOURNEY,

    /** A new instance for each attempt. */
    ATTEMPT,
}

/**
 * A service provider the workflow declares: its name, the key and type of the service it supplies,
 * its scope, and the steps it serves only, each as often as named, or every step until a sentence
 * says otherwise.
 */
internal class ProviderModel(
    val name: String,
    val key: String,
    val type: ServiceType,
    val scope: ProviderScope,
) {
    var steps: List<String> = emptyList()
        private set

    private var attached = false

    /** Attaches the provider to [steps], once. */
    fun servesOnly(steps: List<String>) {
        if (attached)
            throw ModelException("the steps the provider \"$name\" serves are stated twice")
        attached = true
        this.steps = steps
    }
}

/** An input adapter the workflow declares, attached to its steps, each as often as named. */
internal class AdapterModel(val name: String, val steps: List<String>) {
    /** Whether the adapter runs asynchronously. */
    var isAsynchronous = false
}
