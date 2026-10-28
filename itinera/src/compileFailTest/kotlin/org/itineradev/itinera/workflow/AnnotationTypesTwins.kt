package org.itineradev.itinera.workflow

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step

/** What [AnnotationTypesTest] refuses, written so that it compiles. */
internal object AnnotationTypesTwins {
    @Step
    class Charge : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @StepPolicy(name = "audit") class Audit : ItineraStepPolicy

    @WorkflowPolicy(name = "tell") class Tell : ItineraWorkflowPolicy

    @RescueStep
    class Renew : ItineraRescueStep {
        fun run(): RescueOutcome = succeed()
    }

    class Printer : Reporter {
        override fun report(event: Event) = Unit
    }

    @Workflow(name = "shop", policies = [Tell::class], reporters = [Printer::class])
    @WithStep(Charge::class, name = "charge", policies = [Audit::class], rescue = Renew::class)
    class Shop : ItineraWorkflow
}
