package org.itineradev.itinera.policy

import org.itineradev.itinera.engine.PolicyDraft
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.StepFailureKind
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.StepRetryKind
import org.itineradev.itinera.workflow.Sync

/**
 * Builds the [StepPolicyDescriptor] of a synchronous step policy, which defines at least one step
 * hook and each at most once.
 *
 * @param P the policy its hooks are called on.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
public class StepPolicyBuilder<P> internal constructor(private val draft: PolicyDraft<P, Sync>) {
    /**
     * Defines `on step success`, called after an attempt succeeded, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepSuccess(
        hook: SyncHook<P, StepHookKind<Sync>, OnSuccess>
    ): StepPolicyBuilder<P> = onStepSuccess(emptyList(), hook)

    /**
     * Defines `on step success`, called after an attempt succeeded, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepSuccess(
        requests: List<Request<StepHookKind<Sync>, *>>,
        hook: SyncHook<P, StepHookKind<Sync>, OnSuccess>,
    ): StepPolicyBuilder<P> = apply { draft.onStepSuccess(requests, hook) }

    /**
     * Defines `on step failure`, called once a step is given up and ends as failed, which requests
     * nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepFailure(
        hook: SyncHook<P, StepFailureKind<Sync>, OnFailure>
    ): StepPolicyBuilder<P> = onStepFailure(emptyList(), hook)

    /**
     * Defines `on step failure`, called once a step is given up and ends as failed, with what it
     * requests, in order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepFailure(
        requests: List<Request<StepFailureKind<Sync>, *>>,
        hook: SyncHook<P, StepFailureKind<Sync>, OnFailure>,
    ): StepPolicyBuilder<P> = apply { draft.onStepFailure(requests, hook) }

    /**
     * Defines `on step retry`, called before a step is attempted again, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepRetry(
        hook: SyncHook<P, StepRetryKind<Sync>, OnFailure>
    ): StepPolicyBuilder<P> = onStepRetry(emptyList(), hook)

    /**
     * Defines `on step retry`, called before a step is attempted again, with what it requests, in
     * order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepRetry(
        requests: List<Request<StepRetryKind<Sync>, *>>,
        hook: SyncHook<P, StepRetryKind<Sync>, OnFailure>,
    ): StepPolicyBuilder<P> = apply { draft.onStepRetry(requests, hook) }

    /**
     * Defines `on step abnormal termination`, called after an attempt ended in an abnormal
     * termination, which requests nothing.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepAbnormalTermination(
        hook: SyncHook<P, StepHookKind<Sync>, OnFailure>
    ): StepPolicyBuilder<P> = onStepAbnormalTermination(emptyList(), hook)

    /**
     * Defines `on step abnormal termination`, called after an attempt ended in an abnormal
     * termination, with what it requests, in order.
     *
     * @throws IllegalStateException when the policy already defines it.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun onStepAbnormalTermination(
        requests: List<Request<StepHookKind<Sync>, *>>,
        hook: SyncHook<P, StepHookKind<Sync>, OnFailure>,
    ): StepPolicyBuilder<P> = apply { draft.onStepAbnormalTermination(requests, hook) }

    /**
     * The descriptor.
     *
     * @throws IllegalStateException when the policy defines no hook.
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun build(): StepPolicyDescriptor<P, Sync> = draft.stepPolicy()
}
