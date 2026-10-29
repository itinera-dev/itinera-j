package org.itineradev.itinera.policy

import org.itineradev.itinera.step.Reason

/**
 * What `on step success` returns: [KeepDefault], so the journey goes on to the next step,
 * [Lifecycle.FinishWorkflow] or [Lifecycle.FailWorkflow], each made with the factories below.
 *
 * @sample org.itineradev.itinera.policy.samples.decisions
 */
public sealed interface OnSuccess {
    /** Whether the hook keeps the default. */
    public val keepsDefault: Boolean

    /** Whether the hook finishes the workflow, so the journey succeeds at once. */
    public val finishesWorkflow: Boolean

    /** Why the hook fails the workflow, or `null` when it does not. */
    public val failsWorkflow: Reason?

    /** Makes what `on step success` returns. */
    public companion object {
        /**
         * Keeps the default: the journey goes on to the next step.
         *
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic public fun keepDefault(): OnSuccess = KeepDefault

        /**
         * Finishes the workflow: the journey succeeds at once.
         *
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic public fun finishWorkflow(): OnSuccess = Lifecycle.FinishWorkflow

        /**
         * Fails the workflow for [reason]: the journey fails at once.
         *
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic
        public fun failWorkflow(reason: Reason): OnSuccess = Lifecycle.FailWorkflow(reason)

        /**
         * Fails the workflow for a reason with [code] alone.
         *
         * @throws IllegalArgumentException when [code] is empty.
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic
        public fun failWorkflow(code: String): OnSuccess = Lifecycle.FailWorkflow(Reason(code))
    }
}
