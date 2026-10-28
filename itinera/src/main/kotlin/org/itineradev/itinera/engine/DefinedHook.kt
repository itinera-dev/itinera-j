package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.HookDefinition
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** A hook a policy defines: which hook, the policy's name, and its definition. */
internal class DefinedHook<H>(
    val hook: H,
    val policy: PolicyName,
    val definition: HookDefinition<*, *, *, *>,
) {
    /** What the hook requests. */
    val requests: List<Request<*, *>>
        get() = definition.requests

    /** The hook as a part of the workflow, as `mode not accepted` names it. */
    override fun toString(): String = "hook $hook of policy $policy"
}

/** The step hooks [policy] defines, in the order the specification lists them. */
internal fun hooksOf(policy: StepPolicyDescriptor<*, *>): List<DefinedHook<StepHook>> =
    listOf(
            StepHook.ON_STEP_SUCCESS to policy.onStepSuccess,
            StepHook.ON_STEP_FAILURE to policy.onStepFailure,
            StepHook.ON_STEP_RETRY to policy.onStepRetry,
            StepHook.ON_STEP_ABNORMAL_TERMINATION to policy.onStepAbnormalTermination,
        )
        .mapNotNull { (hook, definition) -> defined(hook, policy.name, definition) }

/** The workflow hooks [policy] defines, in the order the specification lists them. */
internal fun hooksOf(policy: WorkflowPolicyDescriptor<*, *>): List<DefinedHook<WorkflowHook>> =
    listOf(
            WorkflowHook.ON_WORKFLOW_SUCCESS to policy.onWorkflowSuccess,
            WorkflowHook.ON_WORKFLOW_FAILURE to policy.onWorkflowFailure,
        )
        .mapNotNull { (hook, definition) -> defined(hook, policy.name, definition) }

/** Every hook the policies of [descriptor] and of its steps define, each policy once. */
internal fun hooksOf(descriptor: WorkflowDescriptor<*, *>): List<DefinedHook<*>> =
    descriptor.policies.flatMap(::hooksOf) + stepPoliciesOf(descriptor).flatMap(::hooksOf)

private fun <H> defined(
    hook: H,
    policy: PolicyName,
    definition: HookDefinition<*, *, *, *>?,
): DefinedHook<H>? = if (definition == null) null else DefinedHook(hook, policy, definition)

/** The step policies attached to the steps of [descriptor], each once. */
internal fun stepPoliciesOf(
    descriptor: WorkflowDescriptor<*, *>
): List<StepPolicyDescriptor<*, *>> =
    descriptor.steps.flatMap(StepDescriptor<*>::policies).distinct()
