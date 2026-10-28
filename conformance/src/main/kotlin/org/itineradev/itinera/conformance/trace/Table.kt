package org.itineradev.itinera.conformance.trace

import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.event.EventKind

private val known = columns.toSet() + setOf("event", "data")

/** The line a row of an event table expects. */
internal fun expectedLine(row: Map<String?, String?>): Line {
    val unknown = row.keys.firstOrNull(::isUnknown)
    if (unknown != null) throw ModelException("event tables have no column \"$unknown\"")
    return Line(eventOf(row), cellsOf(row), dataOf(row["data"]), emptyList())
}

private fun eventOf(row: Map<String?, String?>): String {
    val event = row["event"] ?: throw ModelException("$row has no event")
    EventKind.parse(event) ?: throw ModelException("$event is not a kind of event")
    return event
}

private fun isUnknown(column: String?): Boolean = column !in known

private fun cellsOf(row: Map<String?, String?>): Map<String, String> =
    columns.mapNotNull { cellOf(row, it) }.toMap()

private fun cellOf(row: Map<String?, String?>, column: String): Pair<String, String>? {
    val cell = row[column] ?: return null
    return column to cell
}

private fun dataOf(cell: String?): Data =
    when (cell) {
        null -> Data.Unstated
        "none" -> Data.None
        else -> Data.Is(value(cell))
    }
