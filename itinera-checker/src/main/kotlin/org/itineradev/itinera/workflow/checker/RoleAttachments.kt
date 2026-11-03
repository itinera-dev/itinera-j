package org.itineradev.itinera.workflow.checker

/** A role, by its interface's name, that a hook of the policy named [policy] requests. */
private data class RoleRequest(val policy: String, val role: String)

/**
 * A `role not provided` for each role a hook of [workflow]'s policies, or of its steps' policies,
 * requests that the workflow does not provide, once for each policy.
 */
internal fun rolesNotProvided(workflow: ReadWorkflow, origin: Any): List<Finding> =
    (workflow.policies + workflow.steps.flatMap(DeclaredStep::policies))
        .flatMap(::rolesRequested)
        .distinct()
        .filterNot { request -> provides(workflow, request) }
        .map { request -> roleNotProvided(request, origin) }

private fun rolesRequested(policy: ReadPolicy): List<RoleRequest> =
    policy.hooks.flatMap(ReadHook::asks).filterIsInstance<RoleAsked>().map { asked ->
        roleRequest(policy, asked)
    }

private fun roleRequest(policy: ReadPolicy, asked: RoleAsked): RoleRequest =
    RoleRequest(policy.name, asked.type.name)

private fun provides(workflow: ReadWorkflow, request: RoleRequest): Boolean =
    workflow.roles.any { role -> role.isSubtypeOf(request.role) }

private fun roleNotProvided(request: RoleRequest, origin: Any): Finding =
    Named(
        "role not provided",
        "policy ${request.policy} requests role ${request.role}, which the workflow does not " +
            "provide",
        origin,
    )
