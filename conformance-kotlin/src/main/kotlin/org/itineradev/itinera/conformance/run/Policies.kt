package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookScript
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.PolicyBuild
import org.itineradev.itinera.conformance.model.PolicyKind
import org.itineradev.itinera.conformance.model.PolicyModel
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.testing.StepPolicyMock
import org.itineradev.itinera.policy.testing.StepPolicyScript
import org.itineradev.itinera.policy.testing.WorkflowPolicyMock
import org.itineradev.itinera.policy.testing.WorkflowPolicyScript

/** What builds the step policy [policy] scripts, each time a step mock playing its script. */
internal fun stepPolicyFactory(policy: PolicyModel): PolicyFactory<Played<StepPolicyMock>> {
    val script =
        policy.hooks.entries.fold(builtAs(StepPolicyScript.keepingDefault(), policy.build)) {
            script,
            entry ->
            endingOf(script, entry)
        }
    return PolicyFactory { Played(script.build()) }
}

/** What builds the workflow policy [policy] scripts, each time a mock playing its script. */
internal fun workflowPolicyFactory(policy: PolicyModel): PolicyFactory<Played<WorkflowPolicyMock>> {
    val script =
        policy.hooks.entries.fold(builtAs(WorkflowPolicyScript.returning(), policy.build)) {
            script,
            entry ->
            endingOf(script, entry)
        }
    return PolicyFactory { Played(script.build()) }
}

/** Whether [policy] is attached to steps. */
internal fun isStepPolicy(policy: PolicyModel): Boolean = policy.kind == PolicyKind.STEP

/** The step policy among [policies] that the case attaches as [name]. */
internal fun <D> attached(policies: Map<String, D>, name: String): D =
    policies[name] ?: throw ModelException("no step policy \"$name\" is defined")

/** The step hook [hook] names. */
internal fun stepHookOf(hook: Hook): StepHook =
    (hook as? Hook.OfStep)?.hook ?: throw ModelException("$hook is not a step hook")

/** The workflow hook [hook] names. */
internal fun workflowHookOf(hook: Hook): WorkflowHook =
    (hook as? Hook.OfWorkflow)?.hook ?: throw ModelException("$hook is not a workflow hook")

private fun builtAs(script: StepPolicyScript, build: PolicyBuild): StepPolicyScript =
    when (build) {
        PolicyBuild.Built -> script
        is PolicyBuild.Fails -> script.failingToBuild(build.message ?: UNWRITTEN_FAILURE)
    }

private fun builtAs(script: WorkflowPolicyScript, build: PolicyBuild): WorkflowPolicyScript =
    when (build) {
        PolicyBuild.Built -> script
        is PolicyBuild.Fails -> script.failingToBuild(build.message ?: UNWRITTEN_FAILURE)
    }

private fun endingOf(
    script: StepPolicyScript,
    entry: Map.Entry<Hook, HookScript>,
): StepPolicyScript {
    val returns = entry.value.returns
    return when (stepHookOf(entry.key)) {
        StepHook.ON_STEP_SUCCESS -> script.onStepSuccess(successEnding(returns))
        StepHook.ON_STEP_FAILURE -> script.onStepFailure(failureEnding(returns, entry.key))
        StepHook.ON_STEP_RETRY -> script.onStepRetry(failureEnding(returns, entry.key))
        StepHook.ON_STEP_ABNORMAL_TERMINATION ->
            script.onStepAbnormalTermination(failureEnding(returns, entry.key))
    }
}

private fun endingOf(
    script: WorkflowPolicyScript,
    entry: Map.Entry<Hook, HookScript>,
): WorkflowPolicyScript {
    val ending = workflowEnding(entry.value.returns, entry.key)
    return when (workflowHookOf(entry.key)) {
        WorkflowHook.ON_WORKFLOW_SUCCESS -> script.onWorkflowSuccess(ending)
        WorkflowHook.ON_WORKFLOW_FAILURE -> script.onWorkflowFailure(ending)
    }
}
