package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Policy classes' mistakes in Kotlin. */
class KotlinPolicies private constructor() {
    @Role
    interface Customers {
        fun tell(message: String)
    }

    /** Charges, doing nothing else. */
    @Step
    class Charging : ItineraStep {
        fun run(): Outcome = succeed()
    }

    /** Charges, asynchronously. */
    @Step
    class AsyncCharging : ItineraStep {
        suspend fun run(): Outcome = succeed()
    }

    @StepPolicy(name = "lenient")
    class LenientPolicy : ItineraStepPolicy {
        @OnStepFailure
        fun failed(reason: Reason?): OnFailure = reason?.let { failWorkflow(it) } ?: keepDefault()
    }

    @WithStep(Charging::class, name = "charge", policies = [LenientPolicy::class])
    class LenientAuditing : ItineraWorkflow

    @StepPolicy(name = "audit")
    class AsyncAudit : ItineraStepPolicy {
        @OnStepSuccess
        suspend fun charged(reporter: AsyncHookReporter): OnSuccess {
            reporter.info("audited")
            return keepDefault()
        }
    }

    @WithStep(AsyncCharging::class, name = "charge", policies = [AsyncAudit::class])
    class AsyncAuditing : ItineraAsyncWorkflow

    @StepPolicy(name = "suspend")
    class SuspendingPolicy : ItineraStepPolicy {
        @OnStepSuccess suspend fun charged(): OnSuccess = keepDefault()
    }

    @WithStep(Charging::class, name = "charge", policies = [SuspendingPolicy::class])
    class SuspendingAuditing : ItineraWorkflow

    @StepPolicy(name = "maybe")
    class MaybePolicy : ItineraStepPolicy {
        @OnStepSuccess fun charged(): OnSuccess? = keepDefault()
    }

    @WithStep(Charging::class, name = "charge", policies = [MaybePolicy::class])
    class MaybeDeciding : ItineraWorkflow

    @StepPolicy(name = "finishing")
    class FinishingPolicy : ItineraStepPolicy {
        @OnStepFailure fun failed(): OnSuccess = finishWorkflow()
    }

    @WithStep(Charging::class, name = "charge", policies = [FinishingPolicy::class])
    class Finishing : ItineraWorkflow

    @WorkflowPolicy(name = "maybe tell")
    class MaybeTell : ItineraWorkflowPolicy {
        @OnWorkflowSuccess
        fun paid(@Role customers: Customers?) {
            customers?.tell("paid")
        }
    }

    @Workflow(name = "maybe telling", policies = [MaybeTell::class])
    @WithStep(Charging::class, name = "charge")
    class MaybeTelling : ItineraWorkflow, Customers {
        override fun tell(message: String) = Unit
    }

    @StepPolicy(name = "reporting")
    class AsyncReportingPolicy : ItineraStepPolicy {
        @OnStepSuccess
        fun charged(@Suppress("UnusedParameter") reporter: AsyncHookReporter): OnSuccess =
            keepDefault()
    }

    @WithStep(Charging::class, name = "charge", policies = [AsyncReportingPolicy::class])
    class HookReportingAsynchronously : ItineraWorkflow

    @StepPolicy(name = "reporting")
    class SyncReportingPolicy : ItineraStepPolicy {
        @OnStepSuccess
        suspend fun charged(reporter: HookReporter): OnSuccess {
            reporter.info("charged")
            return keepDefault()
        }
    }

    @WithStep(AsyncCharging::class, name = "charge", policies = [SyncReportingPolicy::class])
    class HookReportingSynchronously : ItineraAsyncWorkflow

    @StepPolicy(name = "inner")
    inner class InnerPolicy : ItineraStepPolicy {
        @OnStepSuccess fun charged(): OnSuccess = keepDefault()
    }

    @WithStep(Charging::class, name = "charge", policies = [InnerPolicy::class])
    class InnerAuditing : ItineraWorkflow

    @StepPolicy(name = "bound")
    class BoundRolePolicy : ItineraStepPolicy {
        @OnStepFailure
        fun <C : Customers> failed(@Role customers: C): OnFailure {
            customers.tell("failed")
            return keepDefault()
        }
    }

    @WithStep(Charging::class, name = "charge", policies = [BoundRolePolicy::class])
    class BoundRoleAuditing : ItineraWorkflow, Customers {
        override fun tell(message: String) = Unit
    }
}
