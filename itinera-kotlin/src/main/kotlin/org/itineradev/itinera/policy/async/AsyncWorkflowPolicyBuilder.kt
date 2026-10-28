package org.itineradev.itinera.policy.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.PolicyDraft
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.WorkflowHookKind
import org.itineradev.itinera.workflow.Async

/**
 * Builds the [WorkflowPolicyDescriptor] of an asynchronous workflow policy, which defines at least
 * one workflow hook and each at most once.
 *
 * @param P the policy its hooks are called on.
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
@ExperimentalItineraApi
public class AsyncWorkflowPolicyBuilder<P>
internal constructor(private val draft: PolicyDraft<P, Async>) {
    /**
     * Defines `on workflow success`, called once the journey succeeded, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onWorkflowSuccess(
        hook: AsyncWorkflowHook<P, WorkflowHookKind<Async>>
    ): AsyncWorkflowPolicyBuilder<P> = onWorkflowSuccess(emptyList(), hook)

    /**
     * Defines `on workflow success`, called once the journey succeeded, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onWorkflowSuccess(
        requests: List<Request<WorkflowHookKind<Async>, *>>,
        hook: AsyncWorkflowHook<P, WorkflowHookKind<Async>>,
    ): AsyncWorkflowPolicyBuilder<P> = apply { draft.onWorkflowSuccess(requests, hook) }

    /**
     * Defines `on workflow failure`, called once the journey failed, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onWorkflowFailure(
        hook: AsyncWorkflowHook<P, WorkflowHookKind<Async>>
    ): AsyncWorkflowPolicyBuilder<P> = onWorkflowFailure(emptyList(), hook)

    /**
     * Defines `on workflow failure`, called once the journey failed, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onWorkflowFailure(
        requests: List<Request<WorkflowHookKind<Async>, *>>,
        hook: AsyncWorkflowHook<P, WorkflowHookKind<Async>>,
    ): AsyncWorkflowPolicyBuilder<P> = apply { draft.onWorkflowFailure(requests, hook) }

    /**
     * The descriptor.
     *
     * @throws IllegalStateException when the policy defines no hook.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun build(): WorkflowPolicyDescriptor<P, Async> = draft.workflowPolicy()
}

/**
 * Starts the descriptor of an asynchronous workflow policy named [name], which [factory] builds
 * once per journey.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
@ExperimentalItineraApi
public fun <P> asyncWorkflowPolicy(
    name: String,
    factory: PolicyFactory<P>,
): AsyncWorkflowPolicyBuilder<P> =
    AsyncWorkflowPolicyBuilder(PolicyDraft(PolicyName.of(name), factory))
