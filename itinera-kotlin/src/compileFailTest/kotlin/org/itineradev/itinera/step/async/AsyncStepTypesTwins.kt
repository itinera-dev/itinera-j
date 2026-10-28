package org.itineradev.itinera.step.async

import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.workflow.async.asyncStep

/** What [AsyncStepTypesTest] refuses, written so that it compiles. */
internal object AsyncStepTypesTwins {
    val run = AsyncStepRun { Outcome.success() }
    val pack = asyncStep("pack") { run }
}
