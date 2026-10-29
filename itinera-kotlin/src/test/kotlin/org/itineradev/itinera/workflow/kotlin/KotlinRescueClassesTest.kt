@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.kotlin

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.RescueKind
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
import org.itineradev.itinera.step.SyncRescueFactory
import org.itineradev.itinera.step.async.AsyncRescueFactory
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.annotated.samples.renewing
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinRescueClassesTest {
    @Test
    fun `a Kotlin rescue step requests its constructor's parameters, then its run's`() {
        assertEquals(
            listOf(
                "input card: String",
                "optional data from the step answer: String?",
                "optional reason",
                "contributor",
            ),
            renewing().rescue?.requests?.map(Request<*, *>::toString),
        )
    }

    @ParameterizedTest
    @FieldSource("declined")
    fun `a Kotlin rescue step is built from its answers and run`(
        reason: Reason?,
        outcome: RescueOutcome,
        contributed: List<Pair<String, Any?>>,
    ) {
        val renewal = renewing().rescue
        val renewed = mutableListOf<Pair<String, Any?>>()
        val contributor = Contributor { key, value -> renewed += key to value }
        val answers = listOf("visa", "declined", reason, contributor)
        val got = Answers<RescueKind<Sync>>(renewal?.requests.orEmpty().zip(answers).toMap())
        assertEquals(outcome, (renewal?.factory as SyncRescueFactory).build(got).run())
        assertEquals(contributed, renewed)
    }

    @Test
    fun `a Kotlin rescue step requests the failure's reason and error by their types`() {
        val rescue = WorkflowDescriptor.of(Failures::class.java).steps.single().rescue
        assertEquals(
            listOf("reason", "optional error", "contributor"),
            rescue?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `an asynchronous Kotlin rescue step is awaited`() = runBlocking {
        val rescue = asyncDescriptorOf(AsyncRenewals::class).steps.single().rescue
        val (card, contributor) = rescue?.requests.orEmpty()
        val renewed = mutableListOf<Pair<String, Any?>>()
        val got =
            Answers<RescueKind<Async>>(
                mapOf(
                    card to "visa",
                    contributor to Contributor { key, value -> renewed += key to value },
                )
            )
        assertEquals(
            RescueOutcome.Success,
            (rescue?.factory as AsyncRescueFactory).build(got).run(),
        )
        assertEquals(listOf("card" to "visa renewed"), renewed)
    }

    @Test
    fun `an annotated step with a rescue step and retries is refused`() {
        val refused = assertThrows<Violations> { WorkflowDescriptor.of(Retried::class.java) }
        assertEquals(listOf(Violation.RescuedStepRetried(StepName.of("bill"))), refused.violations)
    }

    @ParameterizedTest
    @FieldSource("otherModes")
    fun `a Kotlin rescue step of the other mode is refused as not accepted`(
        describe: () -> Any,
        refused: Violation,
    ) {
        assertEquals(listOf(refused), assertThrows<Violations> { describe() }.violations)
    }

    @ParameterizedTest
    @FieldSource("misdeclared")
    fun `every mistake in a Kotlin rescue step is refused when its workflow is described`(
        describe: () -> Any,
        problem: String,
    ) {
        val refused = assertThrows<Violations> { describe() }
        assertEquals(listOf(problem), refused.violations.map(::problem))
    }

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem

    companion object {
        @JvmField
        val declined =
            listOf(
                argumentSet(
                    "with the reason",
                    Reason("expired"),
                    RescueOutcome.Success,
                    listOf("card" to "visa renewed after expired"),
                ),
                argumentSet(
                    "without a reason",
                    null,
                    RescueOutcome.Success,
                    listOf("card" to "visa renewed after declined"),
                ),
                argumentSet(
                    "for a stolen card",
                    Reason("stolen"),
                    RescueOutcome.failure("stolen card"),
                    emptyList<Pair<String, Any?>>(),
                ),
            )

        @JvmField
        val otherModes =
            listOf(
                argumentSet(
                    "a suspend rescue step in a synchronous workflow",
                    describing { WorkflowDescriptor.of(SuspendingRescue::class.java) },
                    Violation.ModeNotAccepted("rescue step bill_rescue", Sync),
                ),
                argumentSet(
                    "a rescue step that is not suspend in an asynchronous workflow",
                    describing { asyncDescriptorOf(PlainAsyncRescue::class) },
                    Violation.ModeNotAccepted("rescue step bill_rescue", Async),
                ),
            )

        @JvmField
        val misdeclared =
            listOf(
                argumentSet(
                    "a rescue step not marked @RescueStep",
                    describing { WorkflowDescriptor.of(Unmarked::class.java) },
                    "is not marked @RescueStep",
                ),
                argumentSet(
                    "a rescue step whose run returns a step's outcome",
                    describing { WorkflowDescriptor.of(Skipping::class.java) },
                    "returns Outcome, a step's outcome, though a rescue step never skips: return " +
                        "RescueOutcome",
                ),
                argumentSet(
                    "a constructor parameter that requests nothing",
                    describing { WorkflowDescriptor.of(Unasked::class.java) },
                    "has a parameter that requests nothing, note: a rescue step's constructor " +
                        "takes inputs, marked @Input, data from the step it rescues, marked " +
                        "@FromRescuedStep, and the failure's Reason and error by their types",
                ),
                argumentSet(
                    "a rescue step with several constructors",
                    describing { WorkflowDescriptor.of(TwiceBuilt::class.java) },
                    "has several constructors, and a rescue step has one",
                ),
                argumentSet(
                    "a run requesting the attempt",
                    describing { WorkflowDescriptor.of(Attempted::class.java) },
                    "has a parameter that requests nothing, attempt: mark a service @Service, " +
                        "and request a handle by its type",
                ),
            )

        private fun describing(describe: () -> Any) = named("describe", describe)
    }
}

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
        delay(1)
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
