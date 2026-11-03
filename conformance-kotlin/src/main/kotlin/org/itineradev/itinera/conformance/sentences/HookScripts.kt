package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookAction
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.HookReturn
import org.itineradev.itinera.conformance.model.HookScript

/** Adds [request] to what [hook] of [policy] requests, after those written before. */
internal fun World.requests(hook: String, policy: String, request: HookRequest) {
    scriptOf(hook, policy).requests += request
}

/** Adds [action] to what [hook] of [policy] does, after those written before. */
internal fun World.does(hook: String, policy: String, action: HookAction) {
    scriptOf(hook, policy).actions += action
}

/** Says how [hook] of [policy] ends. */
internal fun World.ends(hook: String, policy: String, ending: HookReturn) {
    val named = Hook.named(hook)
    model.workflow.policy(policy).hook(named).returns(named, policy, ending)
}

/** The script of [hook] of [policy]. */
internal fun World.scriptOf(hook: String, policy: String): HookScript =
    model.workflow.policy(policy).hook(Hook.named(hook))
