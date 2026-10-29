package org.itineradev.itinera.policy

import org.itineradev.itinera.step.Reason

/**
 * What `on step failure`, `on step retry` and `on step abnormal termination` return: [KeepDefault]
 * or [Lifecycle.FailWorkflow]. A failure cannot finish the workflow, so [Lifecycle.FinishWorkflow]
 * is not one. Each is made with the factories below.
 *
 * @sample org.itineradev.itinera.policy.samples.decisions
 */
public sealed interface OnFailure {
    /** Whether the hook keeps the default. */
    public val keepsDefault: Boolean

    /** Why the hook fails the workflow, or `null` when it does not. */
    public val failsWorkflow: Reason?

    /** Makes what the failure hooks return. */
    public companion object {
        /**
         * Keeps the default, which the hook's place in the journey decides.
         *
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic public fun keepDefault(): OnFailure = KeepDefault

        /**
         * Fails the workflow for [reason]: the journey fails at once.
         *
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic
        public fun failWorkflow(reason: Reason): OnFailure = Lifecycle.FailWorkflow(reason)

        /**
         * Fails the workflow for a reason with [code] alone.
         *
         * @throws IllegalArgumentException when [code] is empty.
         * @sample org.itineradev.itinera.policy.samples.decisions
         */
        @JvmStatic
        public fun failWorkflow(code: String): OnFailure = Lifecycle.FailWorkflow(Reason(code))
    }
}
