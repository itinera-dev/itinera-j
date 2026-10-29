package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByPolicy
import org.itineradev.itinera.event.PolicyCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * A policy built for one attempt of a step, or for one journey: the [policy] its hooks are called
 * on, and the [hooks] it defines.
 */
internal class BuiltPolicy<H>(private val policy: Any?, private val hooks: List<DefinedHook<H>>) {
    /** [hook], as this policy defines it, or `null` when it does not define it. */
    fun defining(hook: H): Defining<H>? {
        val defined = hooks.find { defined -> defined.defines(hook) } ?: return null
        return Defining(policy, defined)
    }
}

private fun <H> DefinedHook<H>.defines(hook: H): Boolean = this.hook == hook

/** A built [policy] that defines [hook], which is called on it. */
internal class Defining<H>(val policy: Any?, val hook: DefinedHook<H>)

/**
 * The step policies attached to [step], each built anew for one of its attempts, in the order they
 * are attached.
 *
 * @throws JourneyAbortSignal when building one fails, aborting the journey with `policy could not
 *   be built`, or gives `null`, aborting it with `unexpected result`.
 */
@JvmSynthetic
internal suspend fun Journey.stepPolicies(
    parts: Parts<*>,
    step: StepDescriptor<*>,
): List<BuiltPolicy<StepHook>> = step.policies.map { policy -> builtFor(parts, step, policy) }

/**
 * The workflow policies [policies], each built once for a journey, in the order they are attached.
 *
 * @throws Refusal when building one fails or gives `null`.
 */
@JvmSynthetic
internal suspend fun workflowPolicies(
    parts: Parts<*>,
    policies: List<WorkflowPolicyDescriptor<*, *>>,
): List<BuiltPolicy<WorkflowHook>> = policies.map { policy -> builtFor(parts, policy) }

private suspend fun Journey.builtFor(
    parts: Parts<*>,
    step: StepDescriptor<*>,
    policy: StepPolicyDescriptor<*, *>,
): BuiltPolicy<StepHook> {
    val built =
        parts
            .calling { policy.factory.build() }
            .getOrElse { thrown ->
                val failure = PolicyCouldNotBeBuilt(step.name, policy.name, messageOf(thrown))
                throw abort(Abort(failure, thrown))
            }
    if (built == null) {
        val message = "building the policy ${policy.name} gave null, not the policy"
        throw abort(Abort(UnexpectedResult(step.name, ByPolicy(policy.name), message), null))
    }
    return BuiltPolicy(built, hooksOf(policy))
}

private suspend fun builtFor(
    parts: Parts<*>,
    policy: WorkflowPolicyDescriptor<*, *>,
): BuiltPolicy<WorkflowHook> {
    val built =
        parts
            .calling { policy.factory.build() }
            .getOrElse { thrown ->
                throw Refusal(Refused.WorkflowPolicyFailed(policy.name, thrown, messageOf(thrown)))
            } ?: throw Refusal(Refused.WorkflowPolicyReturnedNull(policy.name))
    return BuiltPolicy(built, hooksOf(policy))
}
