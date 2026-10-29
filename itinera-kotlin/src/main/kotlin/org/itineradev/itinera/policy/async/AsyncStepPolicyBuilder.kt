package org.itineradev.itinera.policy.async

import org.itineradev.itinera.engine.PolicyDraft
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.workflow.Async

/**
 * Builds the [StepPolicyDescriptor] of an asynchronous step policy, which defines at least one step
 * hook and each at most once.
 *
 * @param P the policy its hooks are called on.
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
public class AsyncStepPolicyBuilder<P : Any>
internal constructor(private val draft: PolicyDraft<P, Async>) {
    /**
     * Defines `on step success`, called after an attempt succeeded, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepSuccess(
        hook: AsyncHook<P, StepHookKind<Async>, OnSuccess>
    ): AsyncStepPolicyBuilder<P> = onStepSuccess(emptyList(), hook)

    /**
     * Defines `on step success`, called after an attempt succeeded, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepSuccess(
        requests: List<Request<StepHookKind<Async>, *>>,
        hook: AsyncHook<P, StepHookKind<Async>, OnSuccess>,
    ): AsyncStepPolicyBuilder<P> = apply { draft.onStepSuccess(requests, hook) }

    /**
     * Defines `on step failure`, called once a step is given up and ends as failed, which requests
     * nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepFailure(
        hook: AsyncHook<P, StepFailureKind<Async>, OnFailure>
    ): AsyncStepPolicyBuilder<P> = onStepFailure(emptyList(), hook)

    /**
     * Defines `on step failure`, called once a step is given up and ends as failed, with what it
     * requests, in order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepFailure(
        requests: List<Request<StepFailureKind<Async>, *>>,
        hook: AsyncHook<P, StepFailureKind<Async>, OnFailure>,
    ): AsyncStepPolicyBuilder<P> = apply { draft.onStepFailure(requests, hook) }

    /**
     * Defines `on step retry`, called before a step is attempted again, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepRetry(
        hook: AsyncHook<P, StepRetryKind<Async>, OnFailure>
    ): AsyncStepPolicyBuilder<P> = onStepRetry(emptyList(), hook)

    /**
     * Defines `on step retry`, called before a step is attempted again, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepRetry(
        requests: List<Request<StepRetryKind<Async>, *>>,
        hook: AsyncHook<P, StepRetryKind<Async>, OnFailure>,
    ): AsyncStepPolicyBuilder<P> = apply { draft.onStepRetry(requests, hook) }

    /**
     * Defines `on step abnormal termination`, called after an attempt ended in an abnormal
     * termination, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepAbnormalTermination(
        hook: AsyncHook<P, StepHookKind<Async>, OnFailure>
    ): AsyncStepPolicyBuilder<P> = onStepAbnormalTermination(emptyList(), hook)

    /**
     * Defines `on step abnormal termination`, called after an attempt ended in an abnormal
     * termination, with what it requests, in order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun onStepAbnormalTermination(
        requests: List<Request<StepHookKind<Async>, *>>,
        hook: AsyncHook<P, StepHookKind<Async>, OnFailure>,
    ): AsyncStepPolicyBuilder<P> = apply { draft.onStepAbnormalTermination(requests, hook) }

    /**
     * The descriptor.
     *
     * @throws IllegalStateException when the policy defines no hook.
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    public fun build(): StepPolicyDescriptor<P, Async> = draft.stepPolicy()
}

/**
 * Starts the descriptor of an asynchronous step policy named [name], which [factory] builds for
 * every attempt.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
public fun <P : Any> asyncStepPolicy(
    name: String,
    factory: PolicyFactory<P>,
): AsyncStepPolicyBuilder<P> = AsyncStepPolicyBuilder(PolicyDraft(PolicyName.of(name), factory))
