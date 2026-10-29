package org.itineradev.itinera.conformance.run

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.conformance.Column
import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookScript
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.journey.testing.Contributions
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.policy.testing.AsyncHookEventReporter
import org.itineradev.itinera.policy.testing.HookEventReporter

/**
 * Calls the hook [hook] of the policy [policy] outside any journey, in [column]'s mode, doing what
 * it does, with a contributor lent by the contributions recorder and a hook reporter lent by the
 * hook event reporter, failing when [model] says so.
 *
 * @throws ModelException when the policy does not define the hook, or the hook requests more than
 *   its handles, which a hook called alone is not given.
 */
internal fun hookRunAlone(
    model: Model,
    witness: Witness,
    policy: String,
    hook: String,
    column: Column,
): RanAlone {
    val script =
        model.workflow.policy(policy).hooks[Hook.named(hook)]
            ?: throw ModelException("the policy \"$policy\" defines no hook \"$hook\"")
    if (script.requests.isNotEmpty()) {
        throw ModelException(
            "the hook \"$hook\" of \"$policy\" requests what a hook run alone is not given"
        )
    }
    val failing = model.testing.hookRecorderFails
    val called = "the hook \"$hook\" of policy \"$policy\""
    return when (column) {
        Column.SYNCHRONOUS -> hookAlone(script, witness, failing, called)
        Column.ASYNCHRONOUS -> runBlocking { asyncHookAlone(script, witness, failing, called) }
    }
}

private fun hookAlone(
    script: HookScript,
    witness: Witness,
    failing: Boolean,
    called: String,
): RanAlone {
    val contributions = Contributions()
    val events = if (failing) HookEventReporter.failing() else HookEventReporter()
    val calls = CarryingOn()
    val reporter = CarriedOnHookReporter(events.reporter, calls)
    val received = HookReceived<HookReporter>(emptyMap(), contributions.contributor, reporter)
    val played = Played(Unit)
    for (action in script.actions) perform(action, received, played, witness)
    return RanAlone(
        called,
        Recorder.HOOK,
        contributions,
        events.emitted,
        events.globalMetadata,
        calls.calls,
    )
}

private suspend fun asyncHookAlone(
    script: HookScript,
    witness: Witness,
    failing: Boolean,
    called: String,
): RanAlone {
    val contributions = Contributions()
    val events = if (failing) AsyncHookEventReporter.failing() else AsyncHookEventReporter()
    val calls = CarryingOn()
    val reporter = CarriedOnAsyncHookReporter(events.reporter, calls)
    val received = HookReceived<AsyncHookReporter>(emptyMap(), contributions.contributor, reporter)
    val played = Played(Unit)
    for (action in script.actions) performAsync(action, received, played, witness)
    return RanAlone(
        called,
        Recorder.HOOK,
        contributions,
        events.emitted,
        events.globalMetadata,
        calls.calls,
    )
}
