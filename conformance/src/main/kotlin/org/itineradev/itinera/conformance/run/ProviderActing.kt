package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.JourneyEmitted
import org.itineradev.itinera.conformance.model.ProviderAction
import org.itineradev.itinera.service.ProviderReporter
import org.itineradev.itinera.service.async.AsyncProviderReporter

/** Does each of [actions], in order, with the synchronous provider reporter [reporter]. */
internal fun perform(actions: List<ProviderAction>, reporter: ProviderReporter) {
    for (action in actions) {
        when (action) {
            is ProviderAction.Emits -> reporter.emit(action.kind, action.message, action.data)
            is ProviderAction.SetsGlobalMetadata ->
                reporter.setGlobalMetadata(action.key, action.value)
        }
    }
}

/** The asynchronous twin of [perform]. */
internal suspend fun performAsync(actions: List<ProviderAction>, reporter: AsyncProviderReporter) {
    for (action in actions) {
        when (action) {
            is ProviderAction.Emits -> reporter.emit(action.kind, action.message, action.data)
            is ProviderAction.SetsGlobalMetadata ->
                reporter.setGlobalMetadata(action.key, action.value)
        }
    }
}

private fun ProviderReporter.emit(kind: JourneyEmitted, message: String, data: Any?) {
    when (kind) {
        JourneyEmitted.JOURNEY_INFO -> info(message, data)
        JourneyEmitted.JOURNEY_WARNING -> warning(message, data)
        JourneyEmitted.JOURNEY_ERROR -> error(message, data)
    }
}

private suspend fun AsyncProviderReporter.emit(kind: JourneyEmitted, message: String, data: Any?) {
    when (kind) {
        JourneyEmitted.JOURNEY_INFO -> info(message, data)
        JourneyEmitted.JOURNEY_WARNING -> warning(message, data)
        JourneyEmitted.JOURNEY_ERROR -> error(message, data)
    }
}
