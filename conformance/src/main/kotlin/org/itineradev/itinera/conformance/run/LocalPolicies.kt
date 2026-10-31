package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookScript
import org.itineradev.itinera.conformance.model.PolicyModel
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.StepPolicyBuilder
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyBuilder
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.testing.StepPolicyMock
import org.itineradev.itinera.policy.testing.WorkflowPolicyMock
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.HookKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.request.WorkflowHookKind
import org.itineradev.itinera.workflow.Sync

/** The step policy [policy] scripts, built with the synchronous builder, its calls [witness]ed. */
internal fun stepPolicyOf(policy: PolicyModel, witness: Witness): StepPolicyDescriptor<*, Sync> {
    val builder = StepPolicyDescriptor.builder(policy.name, stepPolicyFactory(policy))
    for ((hook, script) in policy.hooks) builder.define(policy.name, hook, script, witness)
    return builder.build()
}

/** The workflow policy [policy] scripts, built with the synchronous builder. */
internal fun workflowPolicyOf(
    policy: PolicyModel,
    witness: Witness,
): WorkflowPolicyDescriptor<*, Sync> {
    val builder = WorkflowPolicyDescriptor.builder(policy.name, workflowPolicyFactory(policy))
    for ((hook, script) in policy.hooks) builder.define(policy.name, hook, script, witness)
    return builder.build()
}

private fun StepPolicyBuilder<Played<StepPolicyMock>>.define(
    policy: String,
    hook: Hook,
    script: HookScript,
    witness: Witness,
) {
    val reporter = Requests.hookReporter()
    when (stepHookOf(hook)) {
        StepHook.ON_STEP_SUCCESS -> {
            val scripted = ScriptedHook<Sync, StepHookKind<Sync>, _>(policy, hook, script, reporter)
            onStepSuccess(
                scripted.declared,
                SyncHook { played, got -> played(scripted, played, got, witness).onStepSuccess() },
            )
        }
        StepHook.ON_STEP_FAILURE -> {
            val scripted =
                ScriptedHook<Sync, StepFailureKind<Sync>, _>(policy, hook, script, reporter)
            onStepFailure(
                scripted.declared,
                SyncHook { played, got -> played(scripted, played, got, witness).onStepFailure() },
            )
        }
        StepHook.ON_STEP_RETRY -> {
            val scripted =
                ScriptedHook<Sync, StepRetryKind<Sync>, _>(policy, hook, script, reporter)
            onStepRetry(
                scripted.declared,
                SyncHook { played, got -> played(scripted, played, got, witness).onStepRetry() },
            )
        }
        StepHook.ON_STEP_ABNORMAL_TERMINATION -> {
            val scripted = ScriptedHook<Sync, StepHookKind<Sync>, _>(policy, hook, script, reporter)
            onStepAbnormalTermination(
                scripted.declared,
                SyncHook { played, got ->
                    played(scripted, played, got, witness).onStepAbnormalTermination()
                },
            )
        }
    }
}

private fun WorkflowPolicyBuilder<Played<WorkflowPolicyMock>>.define(
    policy: String,
    hook: Hook,
    script: HookScript,
    witness: Witness,
) {
    val scripted =
        ScriptedHook<Sync, WorkflowHookKind<Sync>, _>(policy, hook, script, Requests.hookReporter())
    when (workflowHookOf(hook)) {
        WorkflowHook.ON_WORKFLOW_SUCCESS ->
            onWorkflowSuccess(
                scripted.declared,
                SyncWorkflowHook { played, got ->
                    played(scripted, played, got, witness).onWorkflowSuccess()
                },
            )
        WorkflowHook.ON_WORKFLOW_FAILURE ->
            onWorkflowFailure(
                scripted.declared,
                SyncWorkflowHook { played, got ->
                    played(scripted, played, got, witness).onWorkflowFailure()
                },
            )
    }
}

/** Plays a call of [hook] on [played] with what [got] gives it, and gives the mock that ends it. */
private fun <K : HookKind<Sync>, T> played(
    hook: ScriptedHook<Sync, K, HookReporter>,
    played: Played<T>,
    got: Got<K>,
    witness: Witness,
): T {
    val received = hook.receivedFrom(got, witness)
    for (action in hook.actions) perform(action, received, played, witness)
    return played.mock
}
