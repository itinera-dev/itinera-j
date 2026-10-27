package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Reason

/**
 * A lifecycle a step hook returned, which decides what happens next, as `hook_called` records it:
 * [FinishWorkflow] or [FailWorkflow]. A hook that keeps the default returns no lifecycle.
 *
 * @sample org.itineradev.itinera.policy.samples.hooks
 */
@ExperimentalItineraApi
public sealed interface Lifecycle {
    /**
     * The journey succeeds at once.
     *
     * @sample org.itineradev.itinera.policy.samples.hooks
     */
    public data object FinishWorkflow : Lifecycle

    /**
     * The journey fails at once, for a reason.
     *
     * @sample org.itineradev.itinera.policy.samples.hooks
     */
    @JvmRecord
    public data class FailWorkflow(
        /** Why the journey fails. */
        val reason: Reason
    ) : Lifecycle
}
