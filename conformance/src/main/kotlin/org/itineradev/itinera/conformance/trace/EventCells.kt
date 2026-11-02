package org.itineradev.itinera.conformance.trace

import org.itineradev.itinera.conformance.run.writtenImplementation
import org.itineradev.itinera.event.AdapterSource
import org.itineradev.itinera.event.ByWorkflowHook
import org.itineradev.itinera.event.ContributionSource
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.MetadataSource
import org.itineradev.itinera.event.OptionalInputAbsent
import org.itineradev.itinera.event.ProviderSource
import org.itineradev.itinera.event.RequestSource
import org.itineradev.itinera.event.ServiceProvided
import org.itineradev.itinera.event.StepHookSource
import org.itineradev.itinera.event.StepSource

/** A column of an event table and what an event writes in it. */
internal typealias Cell = Pair<String, Any?>

/** The cells of [event]: its code, its step, its error's message and what its abort names. */
internal fun cellsOf(event: JourneyAborted): List<Cell> =
    listOf(
        "code" to event.abort.reason,
        "step" to event.abort.step,
        "error" to event.abort.errorMessage,
    ) + cellsOf(event.abort)

/** The cells of [event], whose implementation is written as the cases name it. */
internal fun cellsOf(event: ServiceProvided): List<Cell> =
    listOf(
        "step" to event.step,
        "attempt" to event.attempt,
        "key" to event.key,
        "provider" to event.provider,
        "implementation" to writtenImplementation(event.implementation),
    )

/** The cells of [event]: the key and who requested it. */
internal fun cellsOf(event: OptionalInputAbsent): List<Cell> =
    listOf("key" to event.key) + cellsOf(event.requester)

/** The cells of a contribution under [key]: the key, and who made it. */
internal fun contributedCells(key: String, source: ContributionSource): List<Cell> =
    listOf("key" to key) + cellsOf(source)

/** The cells of an entry of global metadata under [key]: the key, and who set it. */
internal fun metadataCells(key: String, source: MetadataSource): List<Cell> =
    listOf("key" to key) + cellsOf(source)

private fun cellsOf(source: MetadataSource): List<Cell> =
    when (source) {
        is StepSource ->
            listOf("step" to source.step, "attempt" to source.attempt, "source" to source.step)
        is StepHookSource ->
            listOf(
                "step" to source.step,
                "attempt" to source.attempt,
                "policy" to source.policy,
                "hook" to source.hook,
                "source" to "${source.policy}, ${source.hook}",
            )
        is ByWorkflowHook ->
            listOf(
                "policy" to source.policy,
                "hook" to source.hook,
                "source" to "${source.policy}, ${source.hook}",
            )
        is ProviderSource ->
            listOf(
                "step" to source.step,
                "attempt" to source.attempt,
                "provider" to source.provider,
            )
    }

private fun cellsOf(source: ContributionSource): List<Cell> =
    when (source) {
        is StepSource ->
            listOf("step" to source.step, "attempt" to source.attempt, "source" to source.step)
        is StepHookSource ->
            listOf(
                "step" to source.step,
                "attempt" to source.attempt,
                "source" to "${source.policy}, ${source.hook}",
            )
        is ByWorkflowHook -> listOf("source" to "${source.policy}, ${source.hook}")
    }

private fun cellsOf(source: RequestSource): List<Cell> =
    when (source) {
        is StepSource -> listOf("step" to source.step, "attempt" to source.attempt)
        is AdapterSource ->
            listOf("step" to source.step, "attempt" to source.attempt, "adapter" to source.adapter)
        is ProviderSource ->
            listOf(
                "step" to source.step,
                "attempt" to source.attempt,
                "provider" to source.provider,
            )
        is StepHookSource ->
            listOf("step" to source.step, "attempt" to source.attempt) +
                hookCells(source.policy, source.hook)
        is ByWorkflowHook -> hookCells(source.policy, source.hook)
    }
