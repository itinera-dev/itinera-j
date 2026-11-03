package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step

/** Mistakes in Kotlin parts that no workflow names, each read alone in the mode its methods say. */
class KotlinAlone private constructor() {
    /** Has nothing to run. */
    @Step class Runless : ItineraStep

    /** Packs the parcel asynchronously, without a mistake. */
    @Step
    class Pack : ItineraStep {
        suspend fun run(): Outcome = succeed()
    }

    /** Would skip, which a rescue step never does. */
    @RescueStep
    class Skipping : ItineraRescueStep {
        suspend fun run(): Outcome = Outcome.success()
    }

    /** Defines one hook of each mode, so no workflow accepts it. */
    @StepPolicy(name = "mixed")
    class Mixed : ItineraStepPolicy {
        @OnStepSuccess suspend fun charged(): OnSuccess = keepDefault()

        @OnStepFailure fun failed(): OnFailure = keepDefault()
    }

    /** Defines no hook. */
    @WorkflowPolicy(name = "silent") class Silent : ItineraWorkflowPolicy
}
