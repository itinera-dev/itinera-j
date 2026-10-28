package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.policy.HookDefinition
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.request.WorkflowHookKind
import org.itineradev.itinera.workflow.Mode

/**
 * A policy named [name], built by [factory], as the builders of both modes and the annotation
 * readers define its hooks, each at most once.
 *
 * Part of itinera's engine, not of its API.
 */
public class PolicyDraft<P, M : Mode>(
    private val name: PolicyName,
    private val factory: PolicyFactory<P>,
) {
    private var onStepSuccess: HookDefinition<M, P, StepHookKind<M>, OnSuccess>? = null
    private var onStepFailure: HookDefinition<M, P, StepFailureKind<M>, OnFailure>? = null
    private var onStepRetry: HookDefinition<M, P, StepRetryKind<M>, OnFailure>? = null
    private var onStepAbnormalTermination: HookDefinition<M, P, StepHookKind<M>, OnFailure>? = null
    private var onWorkflowSuccess: HookDefinition<M, P, WorkflowHookKind<M>, Unit>? = null
    private var onWorkflowFailure: HookDefinition<M, P, WorkflowHookKind<M>, Unit>? = null

    /**
     * Defines `on step success`.
     *
     * @throws IllegalStateException when the policy already defines it.
     */
    public fun onStepSuccess(
        requests: List<Request<StepHookKind<M>, *>>,
        call: HookCall<M, P, StepHookKind<M>, OnSuccess>,
    ) {
        onStepSuccess = defining(StepHook.ON_STEP_SUCCESS, onStepSuccess, requests, call)
    }

    /**
     * Defines `on step failure`.
     *
     * @throws IllegalStateException when the policy already defines it.
     */
    public fun onStepFailure(
        requests: List<Request<StepFailureKind<M>, *>>,
        call: HookCall<M, P, StepFailureKind<M>, OnFailure>,
    ) {
        onStepFailure = defining(StepHook.ON_STEP_FAILURE, onStepFailure, requests, call)
    }

    /**
     * Defines `on step retry`.
     *
     * @throws IllegalStateException when the policy already defines it.
     */
    public fun onStepRetry(
        requests: List<Request<StepRetryKind<M>, *>>,
        call: HookCall<M, P, StepRetryKind<M>, OnFailure>,
    ) {
        onStepRetry = defining(StepHook.ON_STEP_RETRY, onStepRetry, requests, call)
    }

    /**
     * Defines `on step abnormal termination`.
     *
     * @throws IllegalStateException when the policy already defines it.
     */
    public fun onStepAbnormalTermination(
        requests: List<Request<StepHookKind<M>, *>>,
        call: HookCall<M, P, StepHookKind<M>, OnFailure>,
    ) {
        onStepAbnormalTermination =
            defining(
                StepHook.ON_STEP_ABNORMAL_TERMINATION,
                onStepAbnormalTermination,
                requests,
                call,
            )
    }

    /**
     * Defines `on workflow success`.
     *
     * @throws IllegalStateException when the policy already defines it.
     */
    public fun onWorkflowSuccess(
        requests: List<Request<WorkflowHookKind<M>, *>>,
        call: HookCall<M, P, WorkflowHookKind<M>, Unit>,
    ) {
        onWorkflowSuccess =
            defining(WorkflowHook.ON_WORKFLOW_SUCCESS, onWorkflowSuccess, requests, call)
    }

    /**
     * Defines `on workflow failure`.
     *
     * @throws IllegalStateException when the policy already defines it.
     */
    public fun onWorkflowFailure(
        requests: List<Request<WorkflowHookKind<M>, *>>,
        call: HookCall<M, P, WorkflowHookKind<M>, Unit>,
    ) {
        onWorkflowFailure =
            defining(WorkflowHook.ON_WORKFLOW_FAILURE, onWorkflowFailure, requests, call)
    }

    /**
     * The step policy its step hooks define.
     *
     * @throws IllegalStateException when it defines none.
     */
    public fun stepPolicy(): StepPolicyDescriptor<P, M> {
        check(
            listOfNotNull(onStepSuccess, onStepFailure, onStepRetry, onStepAbnormalTermination)
                .isNotEmpty()
        ) {
            "Policy $name defines no hook, though a policy defines at least one."
        }
        return DefinedStepPolicy(
            name,
            factory,
            onStepSuccess,
            onStepFailure,
            onStepRetry,
            onStepAbnormalTermination,
        )
    }

    /**
     * The workflow policy its workflow hooks define.
     *
     * @throws IllegalStateException when it defines none.
     */
    public fun workflowPolicy(): WorkflowPolicyDescriptor<P, M> {
        check(listOfNotNull(onWorkflowSuccess, onWorkflowFailure).isNotEmpty()) {
            "Policy $name defines no hook, though a policy defines at least one."
        }
        return DefinedWorkflowPolicy(name, factory, onWorkflowSuccess, onWorkflowFailure)
    }

    private fun <K, R> defining(
        hook: Enum<*>,
        defined: HookDefinition<M, P, *, *>?,
        requests: List<Request<K, *>>,
        call: HookCall<M, P, K, R>,
    ): HookDefinition<M, P, K, R> {
        check(defined == null) { "Policy $name already defines $hook." }
        return Defined(requests.toList(), call)
    }
}

private class Defined<M : Mode, P, K, R>(
    override val requests: List<Request<K, *>>,
    override val call: HookCall<M, P, K, R>,
) : HookDefinition<M, P, K, R>

private class DefinedStepPolicy<P, M : Mode>(
    override val name: PolicyName,
    override val factory: PolicyFactory<P>,
    override val onStepSuccess: HookDefinition<M, P, StepHookKind<M>, OnSuccess>?,
    override val onStepFailure: HookDefinition<M, P, StepFailureKind<M>, OnFailure>?,
    override val onStepRetry: HookDefinition<M, P, StepRetryKind<M>, OnFailure>?,
    override val onStepAbnormalTermination: HookDefinition<M, P, StepHookKind<M>, OnFailure>?,
) : StepPolicyDescriptor<P, M>

private class DefinedWorkflowPolicy<P, M : Mode>(
    override val name: PolicyName,
    override val factory: PolicyFactory<P>,
    override val onWorkflowSuccess: HookDefinition<M, P, WorkflowHookKind<M>, Unit>?,
    override val onWorkflowFailure: HookDefinition<M, P, WorkflowHookKind<M>, Unit>?,
) : WorkflowPolicyDescriptor<P, M>
