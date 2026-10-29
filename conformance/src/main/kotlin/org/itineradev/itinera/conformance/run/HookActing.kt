package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.HookAction
import org.itineradev.itinera.conformance.model.JourneyEmitted
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter

/**
 * Does what [action] says with what [received] holds and the synchronous hook reporter, on the
 * policy [played], reaching kept handles through [witness].
 */
internal fun perform(
    action: HookAction,
    received: HookReceived<HookReporter>,
    played: Played<*>,
    witness: Witness,
) {
    val reporter = received.reporter
    when (action) {
        is HookAction.Contributes -> received.contributor.contribute(action.key, action.value)
        is HookAction.CountsCalls -> received.contributor.contribute(action.key, played.counted())
        is HookAction.ChangesStepData -> changeInPlace(received.stepData(action.key), action.value)
        is HookAction.Emits -> reporter.emit(action.kind, action.message)
        is HookAction.SetsGlobalMetadata -> reporter.setGlobalMetadata(action.key, action.value)
        is HookAction.CallsRole -> received.role(action.role).perform(action.operation)
        is HookAction.UsesKeptHandles -> {
            val kept = keptBy(action.step, witness)
            ignoringFailures { kept.contributor.contribute(action.key, action.value) }
            ignoringFailures { (kept.reporter as StepReporter).info(action.message) }
        }
    }
}

/** The asynchronous twin of [perform]. */
internal suspend fun performAsync(
    action: HookAction,
    received: HookReceived<AsyncHookReporter>,
    played: Played<*>,
    witness: Witness,
) {
    val reporter = received.reporter
    when (action) {
        is HookAction.Contributes -> received.contributor.contribute(action.key, action.value)
        is HookAction.CountsCalls -> received.contributor.contribute(action.key, played.counted())
        is HookAction.ChangesStepData -> changeInPlace(received.stepData(action.key), action.value)
        is HookAction.Emits -> reporter.emit(action.kind, action.message)
        is HookAction.SetsGlobalMetadata -> reporter.setGlobalMetadata(action.key, action.value)
        is HookAction.CallsRole -> received.role(action.role).perform(action.operation)
        is HookAction.UsesKeptHandles -> {
            val kept = keptBy(action.step, witness)
            ignoringFailures { kept.contributor.contribute(action.key, action.value) }
            ignoringFailures { (kept.reporter as AsyncStepReporter).info(action.message) }
        }
    }
}

/** What the last attempt of [step] received, whose handles it kept. */
private fun keptBy(step: String, witness: Witness): Received<*> =
    witness.built(step).lastOrNull()?.received
        ?: throw ModelException("\"$step\" was never built, so it kept no handles")

/** Makes [call] on a handle whose attempt has ended, which throws, and carries on. */
private inline fun ignoringFailures(call: () -> Unit) {
    try {
        call()
    } catch (_: IllegalStateException) {}
}

private fun HookReporter.emit(kind: JourneyEmitted, message: String) {
    when (kind) {
        JourneyEmitted.JOURNEY_INFO -> info(message)
        JourneyEmitted.JOURNEY_WARNING -> warning(message)
        JourneyEmitted.JOURNEY_ERROR -> error(message)
    }
}

private suspend fun AsyncHookReporter.emit(kind: JourneyEmitted, message: String) {
    when (kind) {
        JourneyEmitted.JOURNEY_INFO -> info(message)
        JourneyEmitted.JOURNEY_WARNING -> warning(message)
        JourneyEmitted.JOURNEY_ERROR -> error(message)
    }
}
