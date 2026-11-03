package org.itineradev.itinera.workflow.kotlin

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.async.asyncStepPolicyOf
import org.itineradev.itinera.policy.async.asyncWorkflowPolicyOf
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.annotated.samples.describingARescueStep
import org.itineradev.itinera.workflow.annotated.samples.describingAStep
import org.itineradev.itinera.workflow.annotated.samples.describingPolicies
import org.itineradev.itinera.workflow.annotated.samples.refusingAPart
import org.itineradev.itinera.workflow.async.asyncRescueStepOf
import org.itineradev.itinera.workflow.async.asyncStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinPartClassesTest {
    /** What a journey of the shop charged, and what its audit noted. */
    data class Audited(val charged: Long, val audit: String)

    @Test
    fun `a Kotlin step described alone takes its name and retry budget from the builder`() {
        val price = describingAStep()
        assertEquals(StepName.of("price"), price.name)
        assertEquals(2, price.retryBudget)
        assertEquals(
            listOf("input amount: Long", "input discount: Long", "contributor"),
            price.requests.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a Kotlin rescue step described alone requests what its class declares`() {
        assertEquals(
            listOf("input card: String", "contributor"),
            describingARescueStep().rescue?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `Kotlin policies described alone run in a workflow declared by hand`() {
        val events = mutableListOf<Event>()
        val result = describingPolicies(events::add)
        assertEquals(Audited(40L, "charged 40"), result.output(TypeToken.of(Audited::class.java)))
        assertEquals(
            listOf("paid 40"),
            events.filterIsInstance<JourneyInfo>().map(JourneyInfo::message),
        )
    }

    @Test
    fun `a Kotlin policy described alone is named as its annotation says`() {
        assertEquals("audit", StepPolicyDescriptor.of(LoneAudit::class.java).name.toString())
        assertEquals("ledger", WorkflowPolicyDescriptor.of(LoneLedger::class.java).name.toString())
    }

    @Test
    fun `a Kotlin step described alone is refused when its class is not marked @Step`() {
        assertEquals(
            listOf("is not marked @Step"),
            refusingAPart().violations.map(::problem),
        )
    }

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem

    @ParameterizedTest
    @FieldSource("otherModes")
    fun `a Kotlin part described alone in the other mode is refused as not accepted`(
        describe: () -> Any,
        refused: Violation,
    ) {
        assertEquals(listOf(refused), assertThrows<Violations> { describe() }.violations)
    }

    companion object {
        @JvmField
        val otherModes =
            listOf(
                argumentSet(
                    "a suspend step described as synchronous",
                    describing { StepDescriptor.builder("price", LoneSuspendingStep::class.java) },
                    Violation.ModeNotAccepted("step price", Sync),
                ),
                argumentSet(
                    "a step described as asynchronous",
                    describing { asyncStep("price", LoneStep::class) },
                    Violation.ModeNotAccepted("step price", Async),
                ),
                argumentSet(
                    "a suspend rescue step described as synchronous",
                    describing { RescueStepDescriptor.of(LoneSuspendingRescue::class.java) },
                    Violation.ModeNotAccepted("rescue step LoneSuspendingRescue", Sync),
                ),
                argumentSet(
                    "a rescue step described as asynchronous",
                    describing { asyncRescueStepOf(LoneRescue::class) },
                    Violation.ModeNotAccepted("rescue step LoneRescue", Async),
                ),
                argumentSet(
                    "a suspend step policy described as synchronous",
                    describing { StepPolicyDescriptor.of(LoneSuspendingAudit::class.java) },
                    Violation.ModeNotAccepted("hook on step success of policy audit", Sync),
                ),
                argumentSet(
                    "a step policy described as asynchronous",
                    describing { asyncStepPolicyOf(LoneAudit::class) },
                    Violation.ModeNotAccepted("hook on step success of policy audit", Async),
                ),
                argumentSet(
                    "a suspend workflow policy described as synchronous",
                    describing { WorkflowPolicyDescriptor.of(LoneSuspendingLedger::class.java) },
                    Violation.ModeNotAccepted("hook on workflow success of policy ledger", Sync),
                ),
                argumentSet(
                    "a workflow policy described as asynchronous",
                    describing { asyncWorkflowPolicyOf(LoneLedger::class) },
                    Violation.ModeNotAccepted("hook on workflow success of policy ledger", Async),
                ),
            )

        private fun describing(describe: () -> Any) = named("describe", describe)
    }
}

@Step
private class LoneStep : ItineraStep {
    fun run(): Outcome = succeed()
}

@Step
private class LoneSuspendingStep : ItineraStep {
    suspend fun run(): Outcome = succeed()
}

@RescueStep
private class LoneRescue : ItineraRescueStep {
    fun run(): RescueOutcome = succeed()
}

@RescueStep
private class LoneSuspendingRescue : ItineraRescueStep {
    suspend fun run(): RescueOutcome = succeed()
}

@StepPolicy(name = "audit")
private class LoneAudit : ItineraStepPolicy {
    @OnStepSuccess fun charged(): OnSuccess = keepDefault()
}

@StepPolicy(name = "audit")
private class LoneSuspendingAudit : ItineraStepPolicy {
    @OnStepSuccess suspend fun charged(): OnSuccess = keepDefault()
}

@WorkflowPolicy(name = "ledger")
private class LoneLedger : ItineraWorkflowPolicy {
    @OnWorkflowSuccess fun paid() = Unit
}

@WorkflowPolicy(name = "ledger")
private class LoneSuspendingLedger : ItineraWorkflowPolicy {
    @OnWorkflowSuccess suspend fun paid() = Unit
}
