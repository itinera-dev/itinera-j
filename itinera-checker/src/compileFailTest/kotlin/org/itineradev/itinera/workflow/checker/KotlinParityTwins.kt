package org.itineradev.itinera.workflow.checker

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepRetry
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowFailure
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.FromRescuedStep
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/**
 * Workflows with no mistake, in both modes, declaring every part an annotated Kotlin workflow can
 * have, and naming a step written in Java.
 */
class KotlinParityTwins private constructor() {
    @Role
    interface Customers {
        fun tell(message: String)
    }

    fun interface Payments {
        fun take(amount: Long): String
    }

    data class Line(val product: String, val quantity: Long)

    @Step
    class Charge(
        @Input("amount") private val amount: Long,
        @Input("lines") @Suppress("UnusedPrivateProperty") private val lines: List<Line>,
        @Input("note") @Suppress("UnusedPrivateProperty") private val note: String? = null,
    ) : ItineraStep {
        fun run(
            @Service("payments") payments: Payments,
            @Service("count") count: Long,
            contributor: Contributor,
            reporter: StepReporter,
        ): Outcome {
            contributor.contribute("charged", payments.take(amount * count))
            reporter.info("charged")
            return succeed()
        }
    }

    @RescueStep
    class Renew(
        @Input("card") private val card: String,
        @FromRescuedStep("declined") private val declined: String?,
        private val reason: Reason,
        private val error: Throwable?,
    ) : ItineraRescueStep {
        fun run(@Service("payments") payments: Payments): RescueOutcome =
            if (payments.take(1).isEmpty()) fail("$card $declined $reason $error") else succeed()
    }

    @StepPolicy(name = "audit")
    class Audit : ItineraStepPolicy {
        @OnStepSuccess
        fun charged(
            @FromStep("charged") charged: String,
            journey: JourneyId,
            step: StepName,
            attempt: Attempt,
        ): OnSuccess =
            if ("$charged $journey $step $attempt".isEmpty()) finishWorkflow() else keepDefault()

        @OnStepFailure
        fun failed(cause: FailureCause, reporter: HookReporter): OnFailure {
            reporter.info("$cause")
            return keepDefault()
        }

        @OnStepRetry
        fun retried(cause: RetryCause, @Role customers: Customers): OnFailure {
            customers.tell("$cause")
            return keepDefault()
        }
    }

    /** Tells the customer when a workflow fails. */
    abstract class Failing : ItineraWorkflowPolicy {
        @OnWorkflowFailure
        fun failed(@FromWorkflow("order") order: String?, contributor: Contributor) {
            contributor.contribute("failed", order)
        }
    }

    @WorkflowPolicy(name = "tell")
    class Tell : Failing() {
        @OnWorkflowSuccess
        fun paid(@Role customers: Customers, journey: JourneyId) {
            customers.tell("$journey")
        }
    }

    class Printer : Reporter {
        override fun report(event: Event) = Unit
    }

    /** Prices the charge. */
    abstract class Pricing : ItineraWorkflow {
        @InputAdapter(steps = ["charge", "charge_rescue"], name = "pricing")
        fun priced(
            @Key key: String,
            @FromWorkflow("price") price: Long,
        ): Supplied = if (key == "amount") supply(price) else fromDataBag()
    }

    @Workflow(name = "shop", policies = [Tell::class], reporters = [Printer::class])
    @WithStep(Charge::class, name = "charge", rescue = Renew::class, policies = [Audit::class])
    @WithStep(JavaParityTwins.Ship::class, name = "ship", retryBudget = 2)
    class Shop : Pricing(), Customers {
        override fun tell(message: String) = Unit

        @Provider(
            perAttempt = true,
            steps = ["charge", "charge_rescue"],
            key = "payments",
            name = "gateway",
        )
        fun gateway(@FromWorkflow("currency") currency: String): Provided<Payments> =
            provide(Payments { amount -> "$amount $currency" })

        @Provider(key = "count") fun count(): Provided<Long> = provide(1L)

        @Provider(key = "receipts")
        fun receipts(journey: JourneyId): Provided<String> = provide("$journey")

        @IdGenerator fun id(data: DataBag): String = "order ${data.keys.size}"
    }

    @Step
    class Pack : ItineraStep {
        suspend fun run(@Service("boxes") boxes: Long, reporter: AsyncStepReporter): Outcome {
            reporter.info("packed in $boxes")
            return succeed()
        }
    }

    @StepPolicy(name = "watch")
    class Watch : ItineraStepPolicy {
        @OnStepSuccess
        suspend fun packed(reporter: AsyncHookReporter): OnSuccess {
            reporter.info("packed")
            return keepDefault()
        }
    }

    class Quiet : AsyncReporter {
        override suspend fun report(event: Event) = Unit
    }

    @Workflow(name = "store", reporters = [Quiet::class])
    @WithStep(Pack::class, name = "pack", policies = [Watch::class])
    class Store : ItineraAsyncWorkflow {
        @Provider(key = "boxes") suspend fun boxes(): Provided<Long> = provide(2L)

        @InputAdapter(steps = ["pack"]) suspend fun sized(): Supplied = fromDataBag()

        @IdGenerator fun id(): String = javaClass.simpleName
    }
}
