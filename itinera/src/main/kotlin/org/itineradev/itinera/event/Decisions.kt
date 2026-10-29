package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/**
 * The executor decided to attempt a step again. Its own rule always decides it.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepRetrying(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** How the attempt failed. */
    val cause: RetryCause,
) : Decision {
    override val kind: EventKind
        get() = EventKind.STEP_RETRYING

    override val decidedBy: DecidedBy
        get() = DecidedBy.Default
}

/**
 * The executor decided to rescue a step: its rescue step runs, then the step is attempted again.
 * Its own rule always decides it.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepRescuing(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** How the attempt failed. */
    val cause: RescueCause,
    /** The name of the step's rescue step. */
    val rescue: StepName,
) : Decision {
    override val kind: EventKind
        get() = EventKind.STEP_RESCUING

    override val decidedBy: DecidedBy
        get() = DecidedBy.Default
}

/**
 * A step's rescue step succeeded, and the executor decided to attempt the step again. Its own rule
 * always decides it.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepRescued(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The name of the step's rescue step. */
    val rescue: StepName,
) : Decision {
    override val kind: EventKind
        get() = EventKind.STEP_RESCUED

    override val decidedBy: DecidedBy
        get() = DecidedBy.Default
}

/**
 * The executor decided not to attempt a step again: it ends as failed, and the attempt is its last.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepGivenUp(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** Why it will not be attempted again. */
    val cause: GiveUpCause,
) : Decision {
    override val kind: EventKind
        get() = EventKind.STEP_GIVEN_UP

    override val decidedBy: DecidedBy
        get() = decider(cause)
}

/**
 * The executor decided that the journey succeeds: no step was left, or a hook returned
 * `FinishWorkflow`.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneySucceeded(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    override val decidedBy: DecidedBy,
) : Decision {
    override val kind: EventKind
        get() = EventKind.JOURNEY_SUCCEEDED
}

/**
 * The executor decided that the journey fails. It names the step that failed, or whose hook
 * returned `FailWorkflow`, but no attempt.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneyFailed(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The name of the step that failed. */
    val step: StepName,
    /** Why it failed. */
    val failure: JourneyFailure,
) : Decision {
    override val kind: EventKind
        get() = EventKind.JOURNEY_FAILED

    override val decidedBy: DecidedBy
        get() = failure.decidedBy
}

private fun decider(cause: GiveUpCause): DecidedBy =
    when (cause) {
        is GiveUpCause.FailWorkflow -> cause.decidedBy
        GiveUpCause.Failure,
        GiveUpCause.AbnormalTermination,
        GiveUpCause.RetriesExhausted -> DecidedBy.Default
    }
