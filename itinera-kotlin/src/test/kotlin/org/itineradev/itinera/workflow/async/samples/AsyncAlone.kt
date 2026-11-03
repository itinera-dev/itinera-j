package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.instance.async.instance
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.policy.async.asyncStepPolicyOf
import org.itineradev.itinera.policy.async.asyncWorkflowPolicyOf
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncRescueStepOf
import org.itineradev.itinera.workflow.async.asyncStep

suspend fun asyncDescribingParts(reporter: AsyncReporter): JourneyResult {
    /** Charges the card, which the bank declines until it is renewed. */
    @Step
    class Charge(@Input private val card: String, @Input private val amount: Long) : ItineraStep {
        suspend fun run(contributor: Contributor): Outcome {
            delay(1)
            if (!card.endsWith("renewed")) return fail("declined")
            contributor.contribute("charged", amount)
            return succeed()
        }
    }

    /** Renews the declined card. */
    @RescueStep
    class Renew(@Input private val card: String) : ItineraRescueStep {
        suspend fun run(contributor: Contributor): RescueOutcome {
            delay(1)
            contributor.contribute("card", "$card renewed")
            return succeed()
        }
    }

    /** Notes what each charge was. */
    @StepPolicy(name = "audit")
    class Audit : ItineraStepPolicy {
        @OnStepSuccess
        suspend fun charged(@FromStep charged: Long, contributor: Contributor): OnSuccess {
            delay(1)
            contributor.contribute("audit", "charged $charged")
            return keepDefault()
        }
    }

    /** Reports what the journey charged once it succeeds. */
    @WorkflowPolicy(name = "ledger")
    class Ledger : ItineraWorkflowPolicy {
        @OnWorkflowSuccess
        suspend fun paid(@FromWorkflow charged: Long, reporter: AsyncHookReporter) {
            reporter.info("paid $charged")
        }
    }

    /** The shop whose orders the workflow takes. */
    class Shop

    val charge =
        asyncStep("charge", Charge::class)
            .rescue(asyncRescueStepOf(Renew::class))
            .policy(asyncStepPolicyOf(Audit::class))
            .build()
    val shop =
        asyncBuilder<Shop>("shop").step(charge).policy(asyncWorkflowPolicyOf(Ledger::class)).build()
    val instance =
        shop.instance(Shop()).data("card", "4242").data("amount", 40L).reporter(reporter).create()
    return AsyncLocalExecutor().run(instance)
}
