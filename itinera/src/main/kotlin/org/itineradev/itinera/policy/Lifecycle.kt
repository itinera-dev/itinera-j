package org.itineradev.itinera.policy

import org.itineradev.itinera.step.Reason

/**
 * A lifecycle a step hook returned, which decides what happens next, as `hook_called` records it:
 * [FinishWorkflow] or [FailWorkflow]. A hook that keeps the default returns [KeepDefault], which is
 * no lifecycle.
 *
 * @sample org.itineradev.itinera.policy.samples.hooks
 */
public sealed interface Lifecycle {
    /**
     * The journey succeeds at once.
     *
     * @sample org.itineradev.itinera.policy.samples.hooks
     */
    public data object FinishWorkflow : Lifecycle, OnSuccess {
        override val keepsDefault: Boolean
            get() = false

        override val finishesWorkflow: Boolean
            get() = true

        override val failsWorkflow: Reason?
            get() = null
    }

    /**
     * The journey fails at once, for a reason.
     *
     * @sample org.itineradev.itinera.policy.samples.hooks
     */
    @JvmRecord
    public data class FailWorkflow(
        /** Why the journey fails. */
        val reason: Reason
    ) : Lifecycle, OnSuccess, OnFailure {
        override val keepsDefault: Boolean
            get() = false

        override val finishesWorkflow: Boolean
            get() = false

        override val failsWorkflow: Reason
            get() = reason
    }
}
