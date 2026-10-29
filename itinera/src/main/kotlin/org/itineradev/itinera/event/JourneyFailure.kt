package org.itineradev.itinera.event

import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.step.Reason

/**
 * Why a journey failed, as `journey_failed` tells it: its cause, the reason a step or hook gave or
 * the message of the error that ended the last attempt, and who decided it.
 *
 * @sample org.itineradev.itinera.event.samples.failures
 */
public sealed interface JourneyFailure {
    /** The cause, as the specification names it. */
    public val cause: FailureCause

    /** The reason the last attempt reported, or `FailWorkflow` carried, if any. */
    public val reason: Reason?

    /** The message of the error that ended the last attempt, if one did. */
    public val errorMessage: String?

    /** Who decided it. */
    public val decidedBy: DecidedBy

    /**
     * The step's last attempt reported a failure, and the step has no retry budget.
     *
     * @sample org.itineradev.itinera.event.samples.failures
     */
    @JvmRecord
    public data class Failure(override val reason: Reason) : JourneyFailure {
        override val cause: FailureCause
            get() = FailureCause.FAILURE

        override val errorMessage: String?
            get() = null

        override val decidedBy: DecidedBy
            get() = DecidedBy.Default
    }

    /**
     * The step's last attempt ended in an abnormal termination that is not retried.
     *
     * @sample org.itineradev.itinera.event.samples.failures
     */
    @JvmRecord
    public data class AbnormalTermination(override val errorMessage: String) : JourneyFailure {
        override val cause: FailureCause
            get() = FailureCause.ABNORMAL_TERMINATION

        override val reason: Reason?
            get() = null

        override val decidedBy: DecidedBy
            get() = DecidedBy.Default
    }

    /**
     * The step spent its retry budget, and its last attempt reported a failure.
     *
     * @sample org.itineradev.itinera.event.samples.failures
     */
    @JvmRecord
    public data class RetriesExhaustedByFailure(override val reason: Reason) : JourneyFailure {
        override val cause: FailureCause
            get() = FailureCause.RETRIES_EXHAUSTED

        override val errorMessage: String?
            get() = null

        override val decidedBy: DecidedBy
            get() = DecidedBy.Default
    }

    /**
     * The step spent its retry budget, and its last attempt ended in an abnormal termination.
     *
     * @sample org.itineradev.itinera.event.samples.failures
     */
    @JvmRecord
    public data class RetriesExhaustedByAbnormalTermination(override val errorMessage: String) :
        JourneyFailure {
        override val cause: FailureCause
            get() = FailureCause.RETRIES_EXHAUSTED

        override val reason: Reason?
            get() = null

        override val decidedBy: DecidedBy
            get() = DecidedBy.Default
    }

    /**
     * A hook of the step returned `FailWorkflow`.
     *
     * @sample org.itineradev.itinera.event.samples.failures
     */
    @JvmRecord
    public data class FailWorkflow(
        override val reason: Reason,
        override val decidedBy: DecidingHook,
    ) : JourneyFailure {
        override val cause: FailureCause
            get() = FailureCause.FAIL_WORKFLOW

        override val errorMessage: String?
            get() = null
    }
}
