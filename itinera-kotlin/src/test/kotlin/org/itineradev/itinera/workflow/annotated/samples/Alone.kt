package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

fun describingAStep(): StepDescriptor<Sync> {
    /** Notes the amount to charge, less the discount. */
    @Step
    class Price(@Input private val amount: Long, @Input private val discount: Long) : ItineraStep {
        fun run(contributor: Contributor): Outcome {
            contributor.contribute("price", amount - discount)
            return succeed()
        }
    }

    return StepDescriptor.builder("price", Price::class.java).retryBudget(2).build()
}

fun describingARescueStep(): StepDescriptor<Sync> {
    /** Charges the card, which the bank declines until it is renewed. */
    @Step
    class Charge(@Input private val card: String) : ItineraStep {
        fun run(): Outcome = if (card.endsWith("renewed")) succeed() else fail("declined")
    }

    /** Renews the declined card. */
    @RescueStep
    class Renew(@Input private val card: String) : ItineraRescueStep {
        fun run(contributor: Contributor): RescueOutcome {
            contributor.contribute("card", "$card renewed")
            return succeed()
        }
    }

    return StepDescriptor.builder("charge", Charge::class.java)
        .rescue(RescueStepDescriptor.of(Renew::class.java))
        .build()
}

fun describingPolicies(reporter: Reporter): JourneyResult {
    /** Notes what each charge was. */
    @StepPolicy(name = "audit")
    class Audit : ItineraStepPolicy {
        @OnStepSuccess
        fun charged(@FromStep charged: Long, contributor: Contributor): OnSuccess {
            contributor.contribute("audit", "charged $charged")
            return keepDefault()
        }
    }

    /** Reports what the journey charged once it succeeds. */
    @WorkflowPolicy(name = "ledger")
    class Ledger : ItineraWorkflowPolicy {
        @OnWorkflowSuccess
        fun paid(@FromWorkflow charged: Long, reporter: HookReporter) {
            reporter.info("paid $charged")
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

    /** The shop whose orders the workflow takes. */
    class Shop

    val charge =
        StepDescriptor.builder("charge", Charge::class.java)
            .policy(StepPolicyDescriptor.of(Audit::class.java))
            .build()
    val shop =
        WorkflowDescriptor.builder(Shop::class.java, "shop")
            .step(charge)
            .policy(WorkflowPolicyDescriptor.of(Ledger::class.java))
            .build()
    return LocalExecutor()
        .run(shop.instance(Shop()).data("amount", 40L).reporter(reporter).create())
}

fun refusingAPart(): Violations {
    /** Charges the amount, though nothing marks it as a step. */
    class Charge(@Input private val amount: Long) : ItineraStep {
        fun run(contributor: Contributor): Outcome {
            contributor.contribute("charged", amount)
            return succeed()
        }
    }

    return try {
        StepDescriptor.builder("charge", Charge::class.java)
        error("The class is not marked @Step, so it is refused.")
    } catch (refused: Violations) {
        refused
    }
}
