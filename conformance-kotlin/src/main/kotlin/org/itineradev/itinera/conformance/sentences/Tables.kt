package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.model.Ending
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.value

/** Refuses a row with a column its table does not define. */
internal fun only(columns: Set<String>, row: Map<String?, String?>) {
    val unknown = row.keys - columns
    if (unknown.isNotEmpty()) throw ModelException("the table has no column $unknown")
}

/** Whether a cell that may only be `yes` says yes; an empty cell is never read. */
internal fun isYes(written: String): Boolean =
    if (written == "yes") true else throw ModelException("$written is not yes")

/** A cell holding a whole number. */
internal fun count(written: String): Int =
    written.toIntOrNull() ?: throw ModelException("$written is not a whole number")

/** A cell a row must fill. */
internal fun cell(row: Map<String?, String?>, column: String): String =
    row[column] ?: throw ModelException("$row has no $column")

/**
 * One row of a step's attempts: its number, its outcome, the reason's code, message and details or
 * the error's message, and what it contributes, as a JSON object.
 */
internal fun attemptOf(row: Map<String?, String?>): ScriptedAttempt =
    ScriptedAttempt(
        number =
            cell(row, "attempt").toIntOrNull()
                ?: throw ModelException("$row has no attempt number"),
        ending = Ending.named(cell(row, "outcome")),
        code = row["code"],
        message = row["message"],
        details = row["details"]?.let(::value),
        contributes = row["contributes"]?.let(::contributionsOf).orEmpty(),
    )

private fun contributionsOf(written: String): Map<String, Any?> =
    (value(written) as? Map<*, *>)?.mapKeys(::keyOf)
        ?: throw ModelException("$written is not a JSON object")

/** A JSON object's key, always a string. */
private fun keyOf(entry: Map.Entry<Any?, Any?>): String = entry.key as String

private val quoted = Regex("\"([^\"]*)\"")

/** The names a sentence lists, each quoted, separated by commas. */
internal fun names(listed: String): List<String> = quoted.findAll(listed).map(::firstGroup).toList()

private fun firstGroup(match: MatchResult): String = match.groupValues[1]
