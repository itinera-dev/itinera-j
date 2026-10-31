package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation

/** A `hook defined twice` for each hook that several policies of [step] define. */
internal fun stepHooksDefinedTwice(step: StepDescriptor<*>): List<Violation> =
    definedTwice(step.policies.flatMap(::hooksOf)).map { (hook, policies) ->
        stepHookDefinedTwice(step, hook, policies)
    }

private fun stepHookDefinedTwice(
    step: StepDescriptor<*>,
    hook: StepHook,
    policies: List<PolicyName>,
): Violation = Violation.StepHookDefinedTwice(step.name, hook, policies)

/** A `hook defined twice` for each hook that several of the workflow's [policies] define. */
internal fun workflowHooksDefinedTwice(
    policies: List<WorkflowPolicyDescriptor<*, *>>
): List<Violation> =
    definedTwice(policies.flatMap(::hooksOf)).map { (hook, defining) ->
        workflowHookDefinedTwice(hook, defining)
    }

private fun workflowHookDefinedTwice(hook: WorkflowHook, policies: List<PolicyName>): Violation =
    Violation.WorkflowHookDefinedTwice(hook, policies)

/** Each hook that several of [hooks] define, with the policies defining it, in order. */
private fun <H> definedTwice(hooks: List<DefinedHook<H>>): List<Pair<H, List<PolicyName>>> =
    hooks
        .groupBy(DefinedHook<H>::hook, DefinedHook<H>::policy)
        .filterValues(::isSharedList)
        .toList()

private fun isSharedList(policies: List<PolicyName>): Boolean = isShared(policies.size)
