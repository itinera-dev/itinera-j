package org.itineradev.itinera.policy

import org.itineradev.itinera.engine.PolicyDraft
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.WorkflowHookKind
import org.itineradev.itinera.workflow.Sync

/**
 * Builds the [WorkflowPolicyDescriptor] of a synchronous workflow policy, which defines at least
 * one workflow hook and each at most once.
 *
 * @param P the policy its hooks are called on.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
public class WorkflowPolicyBuilder<P : Any>
internal constructor(private val draft: PolicyDraft<P, Sync>) {
    /**
     * Defines `on workflow success`, called once the journey succeeded, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onWorkflowSuccess(
        hook: SyncWorkflowHook<P, WorkflowHookKind<Sync>>
    ): WorkflowPolicyBuilder<P> = onWorkflowSuccess(emptyList(), hook)

    /**
     * Defines `on workflow success`, called once the journey succeeded, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onWorkflowSuccess(
        requests: List<Request<WorkflowHookKind<Sync>, *>>,
        hook: SyncWorkflowHook<P, WorkflowHookKind<Sync>>,
    ): WorkflowPolicyBuilder<P> = apply { draft.onWorkflowSuccess(requests, hook) }

    /**
     * Defines `on workflow failure`, called once the journey failed, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onWorkflowFailure(
        hook: SyncWorkflowHook<P, WorkflowHookKind<Sync>>
    ): WorkflowPolicyBuilder<P> = onWorkflowFailure(emptyList(), hook)

    /**
     * Defines `on workflow failure`, called once the journey failed, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onWorkflowFailure(
        requests: List<Request<WorkflowHookKind<Sync>, *>>,
        hook: SyncWorkflowHook<P, WorkflowHookKind<Sync>>,
    ): WorkflowPolicyBuilder<P> = apply { draft.onWorkflowFailure(requests, hook) }

    /**
     * The descriptor.
     *
     * @throws IllegalStateException when the policy defines no hook.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun build(): WorkflowPolicyDescriptor<P, Sync> = draft.workflowPolicy()
}
