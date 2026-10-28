package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The violations of how the policies of [descriptor] and of its steps are put together. */
internal fun policyViolations(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    descriptor.steps.flatMap(::stepHooksDefinedTwice) +
        workflowHooksDefinedTwice(descriptor.policies) +
        rolesNotProvided(descriptor) +
        hookless(descriptor)

private fun rolesNotProvided(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    hooksOf(descriptor).flatMap(::rolesRequested).distinct().filterNot { requested ->
        provided(descriptor.roles, requested.role)
    }

private fun rolesRequested(hook: DefinedHook<*>): List<Violation.RoleNotProvided> =
    hook.requests.map(::askedBy).filterIsInstance<RoleAsked>().map { asked ->
        roleNotProvided(hook.policy, asked)
    }

private fun roleNotProvided(policy: PolicyName, asked: RoleAsked): Violation.RoleNotProvided =
    Violation.RoleNotProvided(policy, asked.type)

/** Whether one of [roles] is [requested], or extends it. */
private fun provided(roles: List<Class<*>>, requested: Class<*>): Boolean =
    roles.any(requested::isAssignableFrom)

private fun hookless(descriptor: WorkflowDescriptor<*, *>): List<Violation> {
    val workflowPolicies =
        descriptor.policies.filter(::definesNoHook).map(WorkflowPolicyDescriptor<*, *>::name)
    val stepPolicies =
        stepPoliciesOf(descriptor).filter(::definesNoHook).map(StepPolicyDescriptor<*, *>::name)
    return (workflowPolicies + stepPolicies).distinct().map(::hookless)
}

private fun definesNoHook(policy: WorkflowPolicyDescriptor<*, *>): Boolean =
    hooksOf(policy).isEmpty()

private fun definesNoHook(policy: StepPolicyDescriptor<*, *>): Boolean = hooksOf(policy).isEmpty()

private fun hookless(policy: PolicyName): Violation =
    Violation.Misdeclared("policy $policy", "defines no hook, though a policy defines at least one")
