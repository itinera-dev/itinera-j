package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.step.Reason

/**
 * Who decided what a decision records: [Default], the executor's own rule, or the [DecidingHook]
 * whose returned lifecycle decided it.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
public sealed interface DecidedBy {
    /**
     * The executor's own rule.
     *
     * @sample org.itineradev.itinera.event.samples.decider
     */
    public data object Default : DecidedBy
}

/**
 * The policy and the step hook whose returned lifecycle decided something. Only step hooks return
 * lifecycles, so only they decide.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@ExperimentalItineraApi
@JvmRecord
public data class DecidingHook(
    /** The policy's name. */
    val policy: PolicyName,
    /** The hook. */
    val hook: StepHook,
) : DecidedBy

/**
 * Why a step will not be attempted again: its last attempt reported a failure, ended in an abnormal
 * termination, or spent the step's retry budget, or a hook returned `FailWorkflow`.
 *
 * @sample org.itineradev.itinera.event.samples.givenUp
 */
@ExperimentalItineraApi
public sealed interface GiveUpCause {
    /** The cause, as the specification names it. */
    public val cause: FailureCause

    /**
     * The last attempt reported a failure, and the step has no retry budget.
     *
     * @sample org.itineradev.itinera.event.samples.givenUp
     */
    public data object Failure : GiveUpCause {
        override val cause: FailureCause
            get() = FailureCause.FAILURE
    }

    /**
     * The last attempt ended in an abnormal termination that is not retried.
     *
     * @sample org.itineradev.itinera.event.samples.givenUp
     */
    public data object AbnormalTermination : GiveUpCause {
        override val cause: FailureCause
            get() = FailureCause.ABNORMAL_TERMINATION
    }

    /**
     * The step's retry budget, greater than 0, is spent.
     *
     * @sample org.itineradev.itinera.event.samples.givenUp
     */
    public data object RetriesExhausted : GiveUpCause {
        override val cause: FailureCause
            get() = FailureCause.RETRIES_EXHAUSTED
    }

    /**
     * A hook of the step returned `FailWorkflow`.
     *
     * @sample org.itineradev.itinera.event.samples.givenUp
     */
    @JvmRecord
    public data class FailWorkflow(
        /** The reason `FailWorkflow` carried. */
        val reason: Reason,
        /** The hook that returned it. */
        val decidedBy: DecidingHook,
    ) : GiveUpCause {
        override val cause: FailureCause
            get() = FailureCause.FAIL_WORKFLOW
    }
}
