package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Builds a policy, as a class's constructor that takes nothing does: a workflow policy once per
 * journey, before anything is emitted, and a step policy for every attempt of a step it is attached
 * to. What it throws refuses the journey, for a workflow policy, or aborts it with `policy could
 * not be built`, for a step policy.
 *
 * @param P the policy it builds, which its hooks are called on.
 * @sample org.itineradev.itinera.policy.samples.policies
 */
@ExperimentalItineraApi
public fun interface PolicyFactory<out P> {
    /**
     * Builds the policy.
     *
     * @sample org.itineradev.itinera.policy.samples.policies
     */
    public fun build(): P
}
