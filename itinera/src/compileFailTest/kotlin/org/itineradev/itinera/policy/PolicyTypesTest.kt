package org.itineradev.itinera.policy

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val POLICIES =
    """
    import org.itineradev.itinera.policy.*
    import org.itineradev.itinera.request.*
    import org.itineradev.itinera.value.TypeToken
    import org.itineradev.itinera.workflow.*

    class Audit

    @Role
    fun interface Messages {
        fun tell(message: String)
    }

    class Shop

    val audit = StepPolicyDescriptor.builder("audit", PolicyFactory(::Audit))
    val notify = WorkflowPolicyDescriptor.builder("notify", PolicyFactory(::Audit))
    """

class PolicyTypesTest {
    @Test
    fun `a failure hook never finishes the workflow`() {
        assertDoesNotCompile(
            "$POLICIES val failing = audit.onStepFailure(SyncHook { _, _ -> OnSuccess.finishWorkflow() })",
            "Return type mismatch: expected 'OnFailure', actual 'OnSuccess'.",
        )
    }

    @ParameterizedTest
    @FieldSource("notLifecycles")
    fun `a step hook returns keepDefault or a lifecycle, and nothing else`(
        hook: String,
        returned: String,
        refused: String,
    ) {
        assertDoesNotCompile(
            "$POLICIES val hooked = audit.$hook { _, _ -> $returned }",
            refused,
        )
    }

    @ParameterizedTest
    @FieldSource("unbuilt")
    fun `a policy's factory always builds a policy`(builder: String) {
        assertDoesNotCompile(
            "$POLICIES val unbuilt = $builder.builder(\"audit\") { null }",
            "Null cannot be a value of a non-null type",
        )
    }

    @ParameterizedTest
    @FieldSource("misplaced")
    fun `a hook requests only what its kind may`(
        hook: String,
        request: String,
        call: String,
        refused: String,
    ) {
        assertDoesNotCompile(
            "$POLICIES val hooked = $hook(listOf($request), $call)",
            "actual type is 'List<$refused>'",
        )
    }

    @ParameterizedTest
    @FieldSource("foreignPolicies")
    fun `a workflow takes only workflow policies of its own mode`(policy: String, refused: String) {
        assertDoesNotCompile(
            "$POLICIES fun attach(workflow: WorkflowBuilder<Shop, Sync>, policy: $policy) = " +
                "workflow.policy(policy)",
            "actual type is '$policy', but '$refused' was expected",
        )
    }

    @Test
    fun `a step takes only the policies of its own mode`() {
        assertDoesNotCompile(
            "$POLICIES fun attach(step: StepBuilder<Sync>, policy: StepPolicyDescriptor<Audit, Async>) = " +
                "step.policy(policy)",
            "actual type is 'StepPolicyDescriptor<Audit, Async>', but " +
                "'StepPolicyDescriptor<*, Sync>' was expected",
        )
    }

    @Test
    fun `a workflow provides only the roles its objects implement`() {
        assertDoesNotCompile(
            "$POLICIES val shop = WorkflowDescriptor.builder(Shop::class.java, \"shop\")" +
                ".role(Messages::class.java)",
            "actual type is 'Class<Messages>', but 'Class<in Shop>' was expected",
        )
    }

    companion object {
        @JvmField
        val notLifecycles =
            listOf(
                argumentSet(
                    "something that is not a lifecycle",
                    "onStepSuccess",
                    "\"not a lifecycle\"",
                    "Return type mismatch: expected 'OnSuccess', actual 'String'.",
                ),
                argumentSet(
                    "nothing",
                    "onStepFailure",
                    "null",
                    "Null cannot be a value of a non-null type 'OnFailure'",
                ),
            )

        @JvmField
        val unbuilt =
            listOf(
                argumentSet("a step policy", "StepPolicyDescriptor"),
                argumentSet("a workflow policy", "WorkflowPolicyDescriptor"),
            )

        @JvmField
        val foreignPolicies =
            listOf(
                argumentSet(
                    "a step policy",
                    "StepPolicyDescriptor<Audit, Sync>",
                    "WorkflowPolicyDescriptor<*, Sync>",
                ),
                argumentSet(
                    "a workflow policy of the other mode",
                    "WorkflowPolicyDescriptor<Audit, Async>",
                    "WorkflowPolicyDescriptor<*, Sync>",
                ),
            )

        @JvmField
        val misplaced =
            listOf(
                argumentSet(
                    "an input in a step hook",
                    "audit.onStepSuccess",
                    "Requests.input(\"amount\", TypeToken.of(Long::class.javaObjectType))",
                    "SyncHook { _, _ -> OnSuccess.keepDefault() }",
                    "Request<BuildKind, Long>",
                ),
                argumentSet(
                    "a service in a step hook",
                    "audit.onStepSuccess",
                    "Requests.service(\"payments\", String::class.java)",
                    "SyncHook { _, _ -> OnSuccess.keepDefault() }",
                    "ServiceRequest<String>",
                ),
                argumentSet(
                    "the failure's cause in on step success",
                    "audit.onStepSuccess",
                    "Requests.failureCause()",
                    "SyncHook { _, _ -> OnSuccess.keepDefault() }",
                    "Request<FailureCauseKind, FailureCause>",
                ),
                argumentSet(
                    "the retry's cause in on step failure",
                    "audit.onStepFailure",
                    "Requests.retryCause()",
                    "SyncHook { _, _ -> OnFailure.keepDefault() }",
                    "Request<RetryCauseKind, RetryCause>",
                ),
                argumentSet(
                    "a step reporter in a step hook",
                    "audit.onStepSuccess",
                    "Requests.stepReporter()",
                    "SyncHook { _, _ -> OnSuccess.keepDefault() }",
                    "Request<StepReporterKind<Sync>, StepReporter>",
                ),
                argumentSet(
                    "the step's name in a workflow hook",
                    "notify.onWorkflowSuccess",
                    "Requests.stepName()",
                    "SyncWorkflowHook { _, _ -> }",
                    "Request<StepNameKind, StepName>",
                ),
                argumentSet(
                    "data from the step in a workflow hook",
                    "notify.onWorkflowFailure",
                    "Requests.fromStep(\"charged\", TypeToken.of(Long::class.javaObjectType))",
                    "SyncWorkflowHook { _, _ -> }",
                    "Request<FromStepKind, Long>",
                ),
            )
    }
}
