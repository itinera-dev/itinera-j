package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Emitted
import org.itineradev.itinera.conformance.model.StepAction
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter

/**
 * Does what [action] says with the contributor and the synchronous step reporter [received] holds,
 * catching what a call on the reporter throws when the step [ignoresEmitFailures].
 */
internal fun perform(
    action: StepAction,
    received: Received<StepReporter>,
    ignoresEmitFailures: Boolean,
) {
    val reporter = received.reporter
    when (action) {
        is StepAction.Contributes -> received.contributor.contribute(action.key, action.value)
        is StepAction.Emits ->
            reporting(ignoresEmitFailures) {
                reporter.emit(action.kind, action.message, action.data)
            }
        is StepAction.SetsGlobalMetadata ->
            reporting(ignoresEmitFailures) { reporter.setGlobalMetadata(action.key, action.value) }
    }
}

/** The asynchronous twin of [perform]. */
internal suspend fun performAsync(
    action: StepAction,
    received: Received<AsyncStepReporter>,
    ignoresEmitFailures: Boolean,
) {
    val reporter = received.reporter
    when (action) {
        is StepAction.Contributes -> received.contributor.contribute(action.key, action.value)
        is StepAction.Emits ->
            reporting(ignoresEmitFailures) {
                reporter.emit(action.kind, action.message, action.data)
            }
        is StepAction.SetsGlobalMetadata ->
            reporting(ignoresEmitFailures) { reporter.setGlobalMetadata(action.key, action.value) }
    }
}

/** Makes [call], catching every exception it throws when [ignoring] its failures. */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
private inline fun reporting(ignoring: Boolean, call: () -> Unit) {
    try {
        call()
    } catch (failure: Exception) {
        if (!ignoring) throw failure
    }
}

private fun StepReporter.emit(kind: Emitted, message: String, data: Any?) {
    when (kind) {
        Emitted.STEP_INFO -> info(message, data)
        Emitted.STEP_WARNING -> warning(message, data)
        Emitted.STEP_ERROR -> error(message, data)
    }
}

private suspend fun AsyncStepReporter.emit(kind: Emitted, message: String, data: Any?) {
    when (kind) {
        Emitted.STEP_INFO -> info(message, data)
        Emitted.STEP_WARNING -> warning(message, data)
        Emitted.STEP_ERROR -> error(message, data)
    }
}
