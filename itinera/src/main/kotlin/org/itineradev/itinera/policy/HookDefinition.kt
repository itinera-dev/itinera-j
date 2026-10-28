package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.workflow.Mode

/**
 * How a policy defines one of its hooks: what the hook requests, in the order declared, and how it
 * is called. A policy builder makes them; a hand-written policy descriptor may implement its own.
 *
 * @param M the mode of the workflows the hook can join.
 * @param P the policy it is called on.
 * @param K the kind of the hook, which says what it may request.
 * @param R what it returns.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
@ExperimentalItineraApi
public interface HookDefinition<M : Mode, in P, K, out R> {
    /** What the hook requests, in the order declared, resolved in that order before it runs. */
    public val requests: List<Request<K, *>>

    /** How the hook is called. */
    public val call: HookCall<M, P, K, R>
}
