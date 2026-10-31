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

/** A request a service provider declares. */
internal sealed interface ProviderRequest {
    /** The name of the step being built. */
    data object StepName : ProviderRequest

    /** The attempt being built. */
    data object Attempt : ProviderRequest

    /** The journey's ID. */
    data object JourneyId : ProviderRequest

    /** Data from the workflow, read from the data bag. */
    data class FromWorkflow(val key: String, val type: ValueType, val optional: Boolean) :
        ProviderRequest
}

/** An implementation of the cases' `labeller` a provider makes. */
internal enum class Implementation(val written: String) {
    FIRST("first labeller"),
    SECOND("second labeller");

    companion object {
        private val byName = entries.associateBy(Implementation::written)

        /** The implementation a case names. */
        fun named(written: String): Implementation =
            byName[written]
                ?: throw ModelException("$written is not an implementation of the cases")
    }
}

/** What a provider supplies as data with each instance it makes. */
internal sealed interface Supplies {
    /** No data. */
    data object Nothing : Supplies

    /** This value. */
    data class Data(val value: Any?) : Supplies

    /** What is not a value. */
    data object NonValue : Supplies
}

/** When a provider fails: on [received] for the request of [key], or on every call. */
internal data class ProviderFailure(
    val message: String,
    val key: String? = null,
    val received: String? = null,
)

/**
 * A service provider the workflow declares: its name, the key and type of the service it supplies,
 * its scope, the steps it serves only, each as often as named, or every step until a sentence says
 * otherwise, and what it requests and does when called.
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

    /** What it requests, in the order written. */
    val requests = mutableListOf<ProviderRequest>()

    /** The key of the request whose string labels each instance, instead of name and count. */
    var labelledBy: String? = null

    /** When it fails, once stated. */
    var failure: ProviderFailure? = null
        set(value) {
            if (field != null)
                throw ModelException("how the provider \"$name\" fails is stated twice")
            field = value
        }

    var implementation = Implementation.FIRST

    var supplies: Supplies = Supplies.Nothing

    /** Whether it returns neither an instance nor an error. */
    var returnsNothing = false

    /** The message every operation of the instances it makes fails with, once stated. */
    var servicesFail: String? = null

    /** Whether it runs asynchronously. */
    var isAsynchronous = false

    /** Attaches the provider to [steps], once. */
    fun servesOnly(steps: List<String>) {
        if (attached)
            throw ModelException("the steps the provider \"$name\" serves are stated twice")
        attached = true
        this.steps = steps
    }
}
