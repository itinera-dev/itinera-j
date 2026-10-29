package org.itineradev.itinera.workflow.checker

/**
 * The mistakes in how the policies of [workflow] and of its steps are put together: each hook that
 * several policies of one step, or of the workflow, define, and each role a hook requests that the
 * workflow does not provide.
 */
internal fun policyMistakes(workflow: ReadWorkflow): List<Finding> {
    val origin = workflow.origin
    return workflow.steps.flatMap { step -> stepHooksDefinedTwice(step, origin) } +
        definedTwice(workflow.policies).map { (hook, policies) ->
            workflowHookDefinedTwice(hook, policies, origin)
        } +
        rolesNotProvided(workflow, origin)
}

private fun stepHooksDefinedTwice(step: DeclaredStep, origin: Any): List<Finding> =
    definedTwice(step.policies).map { (hook, policies) ->
        stepHookDefinedTwice(step.name, hook, policies, origin)
    }

private fun stepHookDefinedTwice(
    step: String,
    hook: Hook,
    policies: List<String>,
    origin: Any,
): Finding =
    Named(
        "hook defined twice",
        "step policies ${policies.joinToString(", ")} of step $step all define ${hook.hook}",
        origin,
    )

private fun workflowHookDefinedTwice(hook: Hook, policies: List<String>, origin: Any): Finding =
    Named(
        "hook defined twice",
        "workflow policies ${policies.joinToString(", ")} all define ${hook.hook}",
        origin,
    )

/** Each hook that several of [policies] define, with the policies defining it, in order. */
private fun definedTwice(policies: List<ReadPolicy>): List<Pair<Hook, List<String>>> =
    policies
        .flatMap(::definitions)
        .groupBy(Pair<Hook, String>::first, Pair<Hook, String>::second)
        .filterValues(::isSharedList)
        .toList()

private fun definitions(policy: ReadPolicy): List<Pair<Hook, String>> =
    policy.hooks.map { hook -> definition(hook, policy) }

private fun definition(hook: ReadHook, policy: ReadPolicy): Pair<Hook, String> =
    hook.hook to policy.name
