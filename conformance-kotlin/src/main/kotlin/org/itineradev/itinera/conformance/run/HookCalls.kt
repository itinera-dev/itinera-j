package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.journey.Contributor

/** One call of a scripted hook: the [hook] of [policy], and what it received for each request. */
internal data class HookCall(
    val policy: String,
    val hook: Hook,
    val received: Map<HookRequest, Any?>,
)

/**
 * What a scripted hook was given for one call: what it requested, by request, its contributor and
 * its hook reporter, of type [R].
 */
internal class HookReceived<R>(
    val requested: Map<HookRequest, Any?>,
    val contributor: Contributor,
    val reporter: R,
) {
    /** The step data it received under [key]. */
    fun stepData(key: String): Any? =
        requested.entries.firstOrNull { entry -> isStepData(entry.key, key) }?.value

    /** The role it received as [role]. */
    fun role(role: String): Notifier =
        requested[HookRequest.Role(role)] as? Notifier
            ?: throw ModelException("the hook calls the role $role it did not request")
}

private fun isStepData(request: HookRequest, key: String): Boolean =
    request is HookRequest.StepData && request.key == key

/** A policy built for a scenario: its mock, and how many calls of its hooks it has seen. */
internal class Played<T>(val mock: T) {
    private var calls = 0L

    /** Counts one more call of its hooks, and gives how many it has seen. */
    fun counted(): Long {
        calls += 1
        return calls
    }
}
