package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookScript
import org.itineradev.itinera.conformance.model.PolicyModel
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.async.AsyncHook
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.policy.async.AsyncStepPolicyBuilder
import org.itineradev.itinera.policy.async.AsyncWorkflowHook
import org.itineradev.itinera.policy.async.AsyncWorkflowPolicyBuilder
import org.itineradev.itinera.policy.async.asyncStepPolicy
import org.itineradev.itinera.policy.async.asyncWorkflowPolicy
import org.itineradev.itinera.policy.testing.StepPolicyMock
import org.itineradev.itinera.policy.testing.WorkflowPolicyMock
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.HookKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.request.WorkflowHookKind
import org.itineradev.itinera.request.async.asyncHookReporter
import org.itineradev.itinera.workflow.Async

/** The step policy [policy] scripts, built with the asynchronous builder, its calls [witness]ed. */
internal fun asyncStepPolicyOf(
    policy: PolicyModel,
    witness: Witness,
): StepPolicyDescriptor<*, Async> {
    val builder = asyncStepPolicy(policy.name, stepPolicyFactory(policy))
    for ((hook, script) in policy.hooks) builder.define(policy.name, hook, script, witness)
    return builder.build()
}

/** The workflow policy [policy] scripts, built with the asynchronous builder. */
internal fun asyncWorkflowPolicyOf(
    policy: PolicyModel,
    witness: Witness,
): WorkflowPolicyDescriptor<*, Async> {
    val builder = asyncWorkflowPolicy(policy.name, workflowPolicyFactory(policy))
    for ((hook, script) in policy.hooks) builder.define(policy.name, hook, script, witness)
    return builder.build()
}

private fun AsyncStepPolicyBuilder<Played<StepPolicyMock>>.define(
    policy: String,
    hook: Hook,
    script: HookScript,
    witness: Witness,
) {
    val reporter = Requests.asyncHookReporter()
    when (stepHookOf(hook)) {
        StepHook.ON_STEP_SUCCESS -> {
            val scripted =
                ScriptedHook<Async, StepHookKind<Async>, _>(policy, hook, script, reporter)
            onStepSuccess(
                scripted.declared,
                AsyncHook { played, got ->
                    playedAsync(scripted, played, got, witness).onStepSuccess()
                },
            )
        }
        StepHook.ON_STEP_FAILURE -> {
            val scripted =
                ScriptedHook<Async, StepFailureKind<Async>, _>(policy, hook, script, reporter)
            onStepFailure(
                scripted.declared,
                AsyncHook { played, got ->
                    playedAsync(scripted, played, got, witness).onStepFailure()
                },
            )
        }
        StepHook.ON_STEP_RETRY -> {
            val scripted =
                ScriptedHook<Async, StepRetryKind<Async>, _>(policy, hook, script, reporter)
            onStepRetry(
                scripted.declared,
                AsyncHook { played, got ->
                    playedAsync(scripted, played, got, witness).onStepRetry()
                },
            )
        }
        StepHook.ON_STEP_ABNORMAL_TERMINATION -> {
            val scripted =
                ScriptedHook<Async, StepHookKind<Async>, _>(policy, hook, script, reporter)
            onStepAbnormalTermination(
                scripted.declared,
                AsyncHook { played, got ->
                    playedAsync(scripted, played, got, witness).onStepAbnormalTermination()
                },
            )
        }
    }
}

private fun AsyncWorkflowPolicyBuilder<Played<WorkflowPolicyMock>>.define(
    policy: String,
    hook: Hook,
    script: HookScript,
    witness: Witness,
) {
    val scripted =
        ScriptedHook<Async, WorkflowHookKind<Async>, _>(
            policy,
            hook,
            script,
            Requests.asyncHookReporter(),
        )
    when (workflowHookOf(hook)) {
        WorkflowHook.ON_WORKFLOW_SUCCESS ->
            onWorkflowSuccess(
                scripted.declared,
                AsyncWorkflowHook { played, got ->
                    playedAsync(scripted, played, got, witness).onWorkflowSuccess()
                },
            )
        WorkflowHook.ON_WORKFLOW_FAILURE ->
            onWorkflowFailure(
                scripted.declared,
                AsyncWorkflowHook { played, got ->
                    playedAsync(scripted, played, got, witness).onWorkflowFailure()
                },
            )
    }
}

/** The asynchronous twin of the synchronous column's `played`. */
private suspend fun <K : HookKind<Async>, T> playedAsync(
    hook: ScriptedHook<Async, K, AsyncHookReporter>,
    played: Played<T>,
    got: Got<K>,
    witness: Witness,
): T {
    val received = hook.receivedFrom(got, witness)
    for (action in hook.actions) performAsync(action, received, played, witness)
    return played.mock
}
