package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/**
 * The journey started.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneyStarted(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The keys of the initial data, never their values. */
    val initialKeys: Set<String>,
) : Event {
    override val kind: EventKind
        get() = EventKind.JOURNEY_STARTED
}

/**
 * An attempt of a step started.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class AttemptStarted(
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
        get() = EventKind.ATTEMPT_STARTED
}

/**
 * An attempt reported Success.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepSucceeded(
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
        get() = EventKind.STEP_SUCCEEDED
}

/**
 * An attempt reported Failure.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepFailed(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** Why the step failed. */
    val reason: Reason,
) : Event {
    override val kind: EventKind
        get() = EventKind.STEP_FAILED
}

/**
 * An attempt reported Skipped.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepSkipped(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** Why the step was skipped, when it said. */
    val reason: Reason?,
) : Event {
    override val kind: EventKind
        get() = EventKind.STEP_SKIPPED
}

/**
 * An error escaped a running step.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepAbnormalTermination(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The error's message. */
    val errorMessage: String,
) : Event {
    override val kind: EventKind
        get() = EventKind.STEP_ABNORMAL_TERMINATION
}

/**
 * The journey was aborted. It is always its last event.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneyAborted(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** Why it was aborted, and what that reason carries. */
    val abort: JourneyAbort,
) : Event {
    override val kind: EventKind
        get() = EventKind.JOURNEY_ABORTED
}
