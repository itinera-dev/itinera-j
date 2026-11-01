package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The violations of the roles the hooks of [descriptor] request: a role the workflow does not
 * provide, and one whose operations itinera may not call.
 */
internal fun roleViolations(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    rolesNotProvided(descriptor) + rolesNotOpen(descriptor)

private fun rolesNotProvided(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    hooksOf(descriptor).flatMap(::rolesRequested).distinct().filterNot { requested ->
        provided(descriptor.roles, requested.role)
    }

private fun rolesNotOpen(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
    hooksOf(descriptor).flatMap(::rolesAskedBy).distinct().filterNot(::open).map(::notOpen)

private fun rolesAskedBy(hook: DefinedHook<*>): List<Class<*>> =
    hook.requests.map(::askedBy).filterIsInstance<RoleAsked>().map(RoleAsked::type)

/** Whether itinera may call every operation of [role] on the workflow. */
private fun open(role: Class<*>): Boolean = role.methods.all(Method::trySetAccessible)

private fun notOpen(role: Class<*>): Violation =
    Violation.Misdeclared(
        role.name,
        "is not open to itinera: open its package to org.itineradev.itinera",
    )

private fun rolesRequested(hook: DefinedHook<*>): List<Violation.RoleNotProvided> =
    hook.requests.map(::askedBy).filterIsInstance<RoleAsked>().map { asked ->
        roleNotProvided(hook.policy, asked)
    }

private fun roleNotProvided(policy: PolicyName, asked: RoleAsked): Violation.RoleNotProvided =
    Violation.RoleNotProvided(policy, asked.type)

/** Whether one of [roles] is [requested], or extends it. */
private fun provided(roles: List<Class<*>>, requested: Class<*>): Boolean =
    roles.any(requested::isAssignableFrom)
