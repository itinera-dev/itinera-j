@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnStepAbnormalTermination
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepRetry
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

fun auditing(): StepDescriptor<Sync> {
    /**
     * Notes what each charge was, and what ended one abnormally. A charge of nothing finishes the
     * journey, and a declined charge, or a retry after an abnormal termination, fails it.
     */
    @StepPolicy(name = "audit")
    class Audit : ItineraStepPolicy {
        @OnStepSuccess
        fun charged(@FromStep charged: Long, contributor: Contributor): OnSuccess {
            contributor.contribute("audit", "charged $charged")
            return if (charged == 0L) finishWorkflow() else keepDefault()
        }

        @OnStepFailure
        fun failed(reason: Reason): OnFailure =
            if (reason.code == "declined") failWorkflow(reason) else keepDefault()

        @OnStepRetry
        fun retried(cause: RetryCause): OnFailure =
            if (cause == RetryCause.ABNORMAL_TERMINATION) failWorkflow("unstable gateway")
            else keepDefault()

        @OnStepAbnormalTermination
        fun crashed(error: Throwable, contributor: Contributor): OnFailure {
            contributor.contribute("audit", "crashed: ${error.message}")
            return keepDefault()
        }
    }

    /** Charges the amount, noting what was charged. */
    @Step
    class Charge(@Input private val amount: Long) : ItineraStep {
        fun run(contributor: Contributor): Outcome {
            contributor.contribute("charged", amount)
            return succeed()
        }
    }

    /** Charges each order, and audits every charge. */
    @Workflow(name = "charging")
    @WithStep(Charge::class, name = "charge", policies = [Audit::class])
    class Charging : ItineraWorkflow

    return WorkflowDescriptor.of(Charging::class.java).steps.single()
}
