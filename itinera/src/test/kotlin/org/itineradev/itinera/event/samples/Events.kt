package org.itineradev.itinera.event.samples

import org.itineradev.itinera.event.AdapterSource
import org.itineradev.itinera.event.ByWorkflowHook
import org.itineradev.itinera.event.DecidedBy
import org.itineradev.itinera.event.DecidingHook
import org.itineradev.itinera.event.Decision
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.ProviderSource
import org.itineradev.itinera.event.RequestSource
import org.itineradev.itinera.event.StepHookSource
import org.itineradev.itinera.event.StepSource

fun describe(event: Event): String {
    val decided = (event as? Decision)?.decidedBy?.let(::decider).orEmpty()
    return "${event.sequence} ${event.timestamp} ${event.journeyId} ${event.kind}$decided"
}

fun decider(decidedBy: DecidedBy): String =
    when (decidedBy) {
        DecidedBy.Default -> ", decided by default"
        is DecidingHook -> ", decided by ${decidedBy.policy}: ${decidedBy.hook}"
    }

fun kinds(written: String): Boolean = EventKind.parse(written) == EventKind.STEP_FAILED

fun sources(source: RequestSource): String =
    when (source) {
        is StepSource -> "step ${source.step}"
        is AdapterSource -> "adapter ${source.adapter} for ${source.step}"
        is StepHookSource -> "${source.policy}: ${source.hook} on ${source.step}"
        is ByWorkflowHook -> "${source.policy}: ${source.hook}"
        is ProviderSource -> "provider ${source.provider} for ${source.step}"
    }
