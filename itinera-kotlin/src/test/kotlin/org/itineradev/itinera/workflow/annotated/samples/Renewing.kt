package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.step.FromRescuedStep
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

fun renewing(): StepDescriptor<Sync> {
    /** Charges the card, noting the bank's answer. */
    @Step
    class Charge(@Input private val card: String) : ItineraStep {
        fun run(contributor: Contributor): Outcome {
            contributor.contribute("answer", "$card charged")
            return succeed()
        }
    }

    /**
     * Renews the card when its charge fails, from the bank's answer the failed charge noted, and
     * the failure's reason when there is one, unless the card was stolen.
     */
    @RescueStep
    class Renew(
        @Input private val card: String,
        @FromRescuedStep("answer") private val answer: String?,
        private val reason: Reason?,
    ) : ItineraRescueStep {
        fun run(contributor: Contributor): RescueOutcome {
            if (reason?.code == "stolen") return fail("stolen card")
            contributor.contribute("card", "$card renewed after ${reason?.code ?: answer}")
            return succeed()
        }
    }

    /** Charges each order's card, renewing a card the bank declines. */
    @Workflow(name = "cards")
    @WithStep(Charge::class, name = "charge", rescue = Renew::class)
    class Cards : ItineraWorkflow

    return WorkflowDescriptor.of(Cards::class.java).steps.single()
}
