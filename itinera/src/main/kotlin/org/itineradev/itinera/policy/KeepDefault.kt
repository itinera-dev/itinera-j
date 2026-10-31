package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Reason

/**
 * What a step hook returns to keep the default: the journey goes on as if no policy defined the
 * hook. It is no lifecycle, so `hook_called` records none.
 *
 * @sample org.itineradev.itinera.policy.samples.decisions
 */
@ExperimentalItineraApi
public data object KeepDefault : OnSuccess, OnFailure {
    override val keepsDefault: Boolean
        get() = true

    override val finishesWorkflow: Boolean
        get() = false

    override val failsWorkflow: Reason?
        get() = null
}
