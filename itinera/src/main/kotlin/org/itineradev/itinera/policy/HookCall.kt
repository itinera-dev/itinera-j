package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync

/**
 * How a hook of a workflow of mode [M] is called: [SyncHook] and [SyncWorkflowHook] in a
 * synchronous workflow, and their asynchronous twins from `itinera-kotlin` in an asynchronous one.
 * A hook definition holds the call of its own mode, so a hook of the other mode cannot join a
 * workflow; the executor checks it again before the journey.
 *
 * @param M the mode of the workflows the hook can join.
 * @param P the policy it is called on.
 * @param K the kind of the hook, which says what it may request.
 * @param R what it returns.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
@ExperimentalItineraApi public interface HookCall<M : Mode, in P, K, out R>

/**
 * A synchronous step hook, called on `policy` with the answers to its requests in `got`. What it
 * throws aborts the journey with `hook failed`.
 *
 * @param P the policy it is called on.
 * @param K the kind of the hook, which says what it may request.
 * @param R what it returns: [OnSuccess] or [OnFailure].
 * @sample org.itineradev.itinera.policy.samples.policies
 */
@ExperimentalItineraApi
public fun interface SyncHook<in P, K, out R> : HookCall<Sync, P, K, R> {
    /**
     * Calls the hook.
     *
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun call(policy: P, got: Got<K>): R
}

/**
 * A synchronous workflow hook, called on `policy` with the answers to its requests in `got`, which
 * returns nothing. What it throws aborts the journey with `hook failed`.
 *
 * @param P the policy it is called on.
 * @param K the kind of the hook, which says what it may request.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
@ExperimentalItineraApi
public fun interface SyncWorkflowHook<in P, K> : HookCall<Sync, P, K, Unit> {
    /**
     * Calls the hook.
     *
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun call(policy: P, got: Got<K>)
}
