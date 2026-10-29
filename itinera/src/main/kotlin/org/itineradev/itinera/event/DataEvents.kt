package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/**
 * A contribution reached the data bag.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class ContributionCommitted(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The contribution's key. */
    val key: String,
    /** Who contributed it. */
    val source: ContributionSource,
) : Event {
    override val kind: EventKind
        get() = EventKind.CONTRIBUTION_COMMITTED
}

/**
 * A skipped step's contributions were discarded.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class ContributionsDiscarded(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
) : Event {
    override val kind: EventKind
        get() = EventKind.CONTRIBUTIONS_DISCARDED
}

/**
 * A committed key replaced an earlier value.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class DataOverwritten(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The key. */
    val key: String,
    /** Who contributed the new value. */
    val source: ContributionSource,
) : Event {
    override val kind: EventKind
        get() = EventKind.DATA_OVERWRITTEN
}

/**
 * A step, a hook or a service provider set an entry of the journey's global metadata, after the
 * reporters received it. The value is never carried: reporters already have it.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class GlobalMetadataSet(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The entry's key. */
    val key: String,
    /** Who set it. */
    val source: MetadataSource,
) : Event {
    override val kind: EventKind
        get() = EventKind.GLOBAL_METADATA_SET
}
