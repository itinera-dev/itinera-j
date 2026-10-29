package org.itineradev.itinera.workflow.kotlin

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.policy.HookDefinition
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.async.AsyncHook
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.annotated.samples.Corner
import org.itineradev.itinera.workflow.annotated.samples.Customers
import org.itineradev.itinera.workflow.annotated.samples.auditing
import org.itineradev.itinera.workflow.annotated.samples.telling
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinPolicyClassesTest {
    private val audit = auditing().policies.single()

    @Test
    fun `a Kotlin step policy's hooks request what their parameters name, in order`() {
        assertEquals(PolicyName.of("audit"), audit.name)
        assertEquals(
            listOf("data from the step charged: Long", "contributor"),
            audit.onStepSuccess?.requests?.map(Request<*, *>::toString),
        )
        assertEquals(listOf("reason"), audit.onStepFailure?.requests?.map(Request<*, *>::toString))
        assertEquals(
            listOf("retry cause"),
            audit.onStepRetry?.requests?.map(Request<*, *>::toString),
        )
        assertEquals(
            listOf("error", "contributor"),
            audit.onStepAbnormalTermination?.requests?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a Kotlin step hook is called on a policy its class builds, with its answers`() {
        val contributed = mutableListOf<Pair<String, Any?>>()
        val decided = succeeded(audit, 40L, recording(contributed))
        assertEquals(KeepDefault, decided)
        assertEquals(listOf("audit" to "charged 40"), contributed)
    }

    @Test
    fun `a Kotlin success hook returns what its method decides`() {
        assertEquals(Lifecycle.FinishWorkflow, succeeded(audit, 0L, recording(mutableListOf())))
    }

    @ParameterizedTest
    @FieldSource("decisions")
    fun `a Kotlin failure hook returns what its method decides`(
        reason: Reason,
        decided: OnFailure,
    ) {
        assertEquals(decided, failed(audit, reason))
    }

    @Test
    fun `a workflow provides every role it implements, which its policies' hooks request`() {
        val orders = telling()
        val tell = orders.policies.single()
        assertEquals(listOf(Customers::class.java), orders.roles)
        assertEquals(PolicyName.of("tell"), tell.name)
        val told = mutableListOf<String>()
        paid(tell, Corner(told), "1")
        assertEquals(listOf("corner: your order 1 is paid"), told)
    }

    @Test
    fun `a Kotlin reason that admits null is optional`() {
        assertEquals(
            listOf("optional reason"),
            WorkflowDescriptor.of(LenientAuditing::class.java)
                .steps
                .single()
                .policies
                .single()
                .onStepFailure
                ?.requests
                ?.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `an asynchronous Kotlin hook is called with its handles`() = runBlocking {
        val policy = asyncDescriptorOf(AsyncAuditing::class).steps.single().policies.single()
        val reporter = AsyncRecording()
        val decided = asyncSucceeded(policy, reporter)
        assertEquals(KeepDefault, decided)
        assertEquals(listOf("audited"), reporter.infos)
    }

    @Test
    fun `a hook of the other mode is not accepted`() {
        val refused =
            assertThrows<Violations> { WorkflowDescriptor.of(SuspendingAuditing::class.java) }
        assertEquals(
            listOf(Violation.ModeNotAccepted("hook on step success of policy suspend", Sync)),
            refused.violations,
        )
    }

    @ParameterizedTest
    @FieldSource("misdeclared")
    fun `every mistake in a Kotlin policy class is refused when it is described`(
        describe: () -> Any,
        problem: String,
    ) {
        val refused = assertThrows<Violations> { describe() }
        assertEquals(listOf(problem), refused.violations.map(::problem))
    }

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem

    private fun recording(contributed: MutableList<Pair<String, Any?>>): Contributor =
        Contributor { key, value ->
            contributed += key to value
        }

    private fun <K> answers(hook: HookDefinition<*, *, K, *>, vararg answers: Any?): Answers<K> =
        Answers(hook.requests.zip(answers).toMap())

    private fun <P> succeeded(
        policy: StepPolicyDescriptor<P, Sync>,
        vararg answers: Any?,
    ): OnSuccess {
        val hook = checkNotNull(policy.onStepSuccess)
        return (hook.call as SyncHook).call(policy.factory.build(), answers(hook, *answers))
    }

    private fun <P> failed(policy: StepPolicyDescriptor<P, Sync>, reason: Reason): OnFailure {
        val hook = checkNotNull(policy.onStepFailure)
        return (hook.call as SyncHook).call(policy.factory.build(), answers(hook, reason))
    }

    private fun <P> paid(policy: WorkflowPolicyDescriptor<P, Sync>, vararg answers: Any?) {
        val hook = checkNotNull(policy.onWorkflowSuccess)
        (hook.call as SyncWorkflowHook).call(policy.factory.build(), answers(hook, *answers))
    }

    private suspend fun <P> asyncSucceeded(
        policy: StepPolicyDescriptor<P, Async>,
        vararg answers: Any?,
    ): OnSuccess {
        val hook = checkNotNull(policy.onStepSuccess)
        return (hook.call as AsyncHook).call(policy.factory.build(), answers(hook, *answers))
    }

    private class AsyncRecording : AsyncHookReporter {
        val infos = mutableListOf<String>()

        override suspend fun info(message: String) {
            infos += message
        }

        override suspend fun info(message: String, data: Any?) = Unit

        override suspend fun warning(message: String) = Unit

        override suspend fun warning(message: String, data: Any?) = Unit

        override suspend fun error(message: String) = Unit

        override suspend fun error(message: String, data: Any?) = Unit

        override suspend fun setGlobalMetadata(key: String, value: Any?) = Unit
    }

    companion object {
        @JvmField
        val decisions =
            listOf(
                argumentSet(
                    "a declined charge fails the workflow",
                    Reason("declined"),
                    Lifecycle.FailWorkflow(Reason("declined")),
                ),
                argumentSet("a timeout keeps the default", Reason("timed out"), KeepDefault),
            )

        @JvmField
        val misdeclared =
            listOf(
                argumentSet(
                    "a hook that may return null",
                    describing { WorkflowDescriptor.of(MaybeDeciding::class.java) },
                    "returns OnSuccess?, which admits null",
                ),
                argumentSet(
                    "a failure hook returning what only a success hook does",
                    describing { WorkflowDescriptor.of(Finishing::class.java) },
                    "returns OnSuccess, not OnFailure",
                ),
                argumentSet(
                    "a role that admits null",
                    describing { WorkflowDescriptor.of(MaybeTelling::class.java) },
                    "requests role Customers as a type that admits null, though a role is " +
                        "always given",
                ),
                argumentSet(
                    "the asynchronous hook reporter in a synchronous hook",
                    describing { WorkflowDescriptor.of(HookReportingAsynchronously::class.java) },
                    "requests AsyncHookReporter, which only an asynchronous hook can",
                ),
                argumentSet(
                    "the synchronous hook reporter in an asynchronous hook",
                    describing { asyncDescriptorOf(HookReportingSynchronously::class) },
                    "requests HookReporter, which only a synchronous hook can",
                ),
            )

        private fun describing(describe: () -> Any) = named("describe", describe)
    }
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
    fun charged(@Suppress("UnusedParameter") reporter: AsyncHookReporter): OnSuccess = keepDefault()
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
