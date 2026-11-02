package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.run.HookCall
import org.itineradev.itinera.conformance.run.holds

/** Checks that a call of [hook] of [policy] received [expected] for the requests [asks] picks. */
internal fun World.received(
    hook: String,
    policy: String,
    asks: (HookRequest) -> Boolean,
    expected: Any?,
) {
    val values = receivedFor(hook, policy, asks)
    holds(expected in values, "\"$hook\" of \"$policy\" to receive $expected", "$values")
}

/** Checks that a call of [hook] of [policy] received a value written as [expected]. */
internal fun World.receivedWritten(
    hook: String,
    policy: String,
    asks: (HookRequest) -> Boolean,
    expected: String,
) {
    val values = receivedFor(hook, policy, asks).map(Any?::toString)
    holds(expected in values, "\"$hook\" of \"$policy\" to receive $expected", "$values")
}

/** What every call of [hook] of [policy] received for the requests [asks] picks. */
internal fun World.receivedFor(
    hook: String,
    policy: String,
    asks: (HookRequest) -> Boolean,
): List<Any?> = callsOf(hook, policy).flatMap { call -> receivedBy(call, asks) }

/** The calls of [hook] of [policy], in the order they were made. */
internal fun World.callsOf(hook: String, policy: String): List<HookCall> {
    val named = Hook.named(hook)
    return witness.hooksCalled.filter { call -> isCallOf(call, named, policy) }
}

private fun isCallOf(call: HookCall, hook: Hook, policy: String): Boolean =
    call.hook == hook && call.policy == policy

private fun receivedBy(call: HookCall, asks: (HookRequest) -> Boolean): Collection<Any?> =
    call.received.filterKeys(asks).values
