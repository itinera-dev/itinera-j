package org.itineradev.itinera.policy.async

import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.workflow.Async

/**
 * An asynchronous step hook, called on `policy` with the answers to its requests in `got`, and
 * awaited. What it throws aborts the journey with `hook failed`, a `CancellationException` of its
 * own included.
 *
 * @param P the policy it is called on.
 * @param K the kind of the hook, which says what it may request.
 * @param R what it returns: `OnSuccess` or `OnFailure`.
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
public fun interface AsyncHook<in P, K, out R> : HookCall<Async, P, K, R> {
    /**
     * Calls the hook.
     *
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    @JvmSynthetic public suspend fun call(policy: P, got: Got<K>): R
}

/**
 * An asynchronous workflow hook, called on `policy` with the answers to its requests in `got`,
 * which returns nothing. What it throws aborts the journey with `hook failed`.
 *
 * @param P the policy it is called on.
 * @param K the kind of the hook, which says what it may request.
 * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
 */
public fun interface AsyncWorkflowHook<in P, K> : HookCall<Async, P, K, Unit> {
    /**
     * Calls the hook.
     *
     * @sample org.itineradev.itinera.policy.async.samples.asyncPolicies
     */
    @JvmSynthetic public suspend fun call(policy: P, got: Got<K>)
}
