package org.itineradev.itinera.engine

import org.itineradev.itinera.workflow.Violation

/** What a journey starts with, captured: its initial data and its initial global metadata. */
internal class Initial(val data: Captures, val metadata: Captures) {
    companion object {
        fun of(data: Map<String, Any?>, metadata: Map<String, Any?>): Initial =
            Initial(
                Captures.of(data, Violation.Held.INITIAL_DATA),
                Captures.of(metadata, Violation.Held.INITIAL_GLOBAL_METADATA),
            )
    }
}

/** The values of a map, captured by key in its order, and the keys whose values are refused. */
internal class Captures
private constructor(val values: Map<String, Captured>, val refused: List<Violation.NotAValue>) {
    companion object {
        fun of(given: Map<String, Any?>, held: Violation.Held): Captures {
            val captured = given.mapValues(::capture)
            return Captures(
                captured.filterValues(::isCaptured).mapValues(::asCaptured),
                captured.filterValues(::isNotAValue).map { entry -> refused(held, entry) },
            )
        }
    }
}

private fun capture(entry: Map.Entry<String, Any?>): Capture = Values.capture(entry.value)

private fun isCaptured(capture: Capture): Boolean = capture is Captured

private fun isNotAValue(capture: Capture): Boolean = capture is NotAValue

private fun asCaptured(entry: Map.Entry<String, Capture>): Captured = entry.value as Captured

private fun refused(held: Violation.Held, entry: Map.Entry<String, Capture>): Violation.NotAValue =
    Violation.NotAValue(held, entry.key, (entry.value as NotAValue).error)
