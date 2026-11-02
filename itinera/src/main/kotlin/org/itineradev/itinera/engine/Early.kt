package org.itineradev.itinera.engine

import org.itineradev.itinera.event.DecidingHook
import org.itineradev.itinera.journey.Failure
import org.itineradev.itinera.step.StepName

/** How a journey ends before its last step is done. */
internal sealed interface Early {
    /** A hook returned `FinishWorkflow`, and the journey succeeds at once, as [by] decided. */
    data class Finished(val by: DecidingHook) : Early

    /** [step] was given up, and the journey fails with [failure]. */
    data class Failed(val step: StepName, val failure: Failure) : Early
}
