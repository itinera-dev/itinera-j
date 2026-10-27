package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.WorkflowName

/**
 * An input adapter supplied a value for a step's input.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
@JvmRecord
public data class InputAdapterSupplied(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The input's key. */
    val key: String,
    /** The adapter's name. */
    val adapter: AdapterName,
) : Event {
    override val kind: EventKind
        get() = EventKind.INPUT_ADAPTER_SUPPLIED
}

/**
 * An input adapter failed for a step's input, or supplied what is not a value.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
@JvmRecord
public data class InputAdapterFailed(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The input's key. */
    val key: String,
    /** The adapter's name. */
    val adapter: AdapterName,
) : Event {
    override val kind: EventKind
        get() = EventKind.INPUT_ADAPTER_FAILED
}

/**
 * An optional request had no value.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
@JvmRecord
public data class OptionalInputAbsent(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The key that was requested. */
    val key: String,
    /** Who requested it. */
    val requester: RequestSource,
) : Event {
    override val kind: EventKind
        get() = EventKind.OPTIONAL_INPUT_ABSENT
}

/**
 * A service provider made an instance of a step's service.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
@JvmRecord
public data class ServiceProvided(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The service's key. */
    val key: String,
    /** The provider's name. */
    val provider: ProviderName,
    /** The implementation the instance is, usually its class's name. */
    val implementation: String,
    /**
     * The data the provider supplied with the instance, captured as a value; `null` when it
     * supplied none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.SERVICE_PROVIDED
}

/**
 * A service provider failed for a step's service, or returned no instance.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
@JvmRecord
public data class ServiceProviderFailed(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The service's key. */
    val key: String,
    /** The provider's name. */
    val provider: ProviderName,
) : Event {
    override val kind: EventKind
        get() = EventKind.SERVICE_PROVIDER_FAILED
}
