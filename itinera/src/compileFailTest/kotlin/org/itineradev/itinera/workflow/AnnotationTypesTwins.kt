package org.itineradev.itinera.workflow

import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step

/** What [AnnotationTypesTest] refuses, written so that it compiles. */
internal object AnnotationTypesTwins {
    @Step
    class Charge : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @StepPolicy(name = "audit") class Audit : ItineraStepPolicy

    @WorkflowPolicy(name = "tell") class Tell : ItineraWorkflowPolicy

    @Workflow(name = "shop", policies = [Tell::class])
    @WithStep(Charge::class, name = "charge", policies = [Audit::class])
    class Shop : ItineraWorkflow
}
