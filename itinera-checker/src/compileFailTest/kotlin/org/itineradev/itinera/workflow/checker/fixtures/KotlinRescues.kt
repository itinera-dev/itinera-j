package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.journey.Contributor
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
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Rescue step classes' mistakes in Kotlin. */
class KotlinRescues private constructor() {
    /** Bills the order. */
    @Step
    class Bill : ItineraStep {
        fun run(): Outcome = succeed()
    }

    /** Renews nothing, and requests the failure's reason and, when there is one, its error. */
    @RescueStep
    class FailureRenewal(private val reason: Reason, private val error: Throwable?) :
        ItineraRescueStep {
        fun run(contributor: Contributor): RescueOutcome {
            contributor.contribute("failure", "${reason.code}: ${error?.message}")
            return succeed()
        }
    }

    @WithStep(Bill::class, name = "bill", rescue = FailureRenewal::class)
    class Failures : ItineraWorkflow

    /** Bills the order asynchronously. */
    @Step
    class AsyncBill : ItineraStep {
        suspend fun run(): Outcome = succeed()
    }

    /** Renews the card asynchronously. */
    @RescueStep
    class AsyncRenewal(@FromRescuedStep("card") private val card: String) : ItineraRescueStep {
        suspend fun run(contributor: Contributor): RescueOutcome {
            contributor.contribute("card", "$card renewed")
            return succeed()
        }
    }

    @WithStep(AsyncBill::class, name = "bill", rescue = AsyncRenewal::class)
    class AsyncRenewals : ItineraAsyncWorkflow

    /** Renews nothing. */
    @RescueStep
    class Renewal : ItineraRescueStep {
        fun run(): RescueOutcome = succeed()
    }

    @WithStep(Bill::class, name = "bill", retryBudget = 1, rescue = Renewal::class)
    class Retried : ItineraWorkflow

    /** Renews nothing, but suspends. */
    @RescueStep
    class SuspendingRenewal : ItineraRescueStep {
        suspend fun run(): RescueOutcome = succeed()
    }

    @WithStep(Bill::class, name = "bill", rescue = SuspendingRenewal::class)
    class SuspendingRescue : ItineraWorkflow

    @WithStep(AsyncBill::class, name = "bill", rescue = Renewal::class)
    class PlainAsyncRescue : ItineraAsyncWorkflow

    /** Renews nothing, and is not marked. */
    class UnmarkedRenewal : ItineraRescueStep {
        fun run(): RescueOutcome = succeed()
    }

    @WithStep(Bill::class, name = "bill", rescue = UnmarkedRenewal::class)
    class Unmarked : ItineraWorkflow

    /** Would skip, as a step can. */
    @RescueStep
    class SkippingRenewal : ItineraRescueStep {
        fun run(): Outcome = Outcome.success()
    }

    @WithStep(Bill::class, name = "bill", rescue = SkippingRenewal::class)
    class Skipping : ItineraWorkflow

    /** Takes a note that it never requests. */
    @RescueStep
    class UnaskedRenewal(private val note: String) : ItineraRescueStep {
        fun run(contributor: Contributor): RescueOutcome {
            contributor.contribute("note", note)
            return succeed()
        }
    }

    @WithStep(Bill::class, name = "bill", rescue = UnaskedRenewal::class)
    class Unasked : ItineraWorkflow

    /** Requests the attempt, which a rescue step never does. */
    @RescueStep
    class AttemptedRenewal : ItineraRescueStep {
        fun run(attempt: Attempt, contributor: Contributor): RescueOutcome {
            contributor.contribute("attempt", attempt.number)
            return succeed()
        }
    }

    @WithStep(Bill::class, name = "bill", rescue = AttemptedRenewal::class)
    class Attempted : ItineraWorkflow

    /** Renews the card with a note, which it can be built with or without. */
    @RescueStep
    class TwiceBuiltRenewal(@Input("note") private val note: String) : ItineraRescueStep {
        constructor() : this("renewed")

        fun run(contributor: Contributor): RescueOutcome {
            contributor.contribute("note", note)
            return succeed()
        }
    }

    @WithStep(Bill::class, name = "bill", rescue = TwiceBuiltRenewal::class)
    class TwiceBuilt : ItineraWorkflow
}
