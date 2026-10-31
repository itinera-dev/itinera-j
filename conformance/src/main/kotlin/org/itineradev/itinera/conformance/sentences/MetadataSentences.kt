package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Entry
import org.itineradev.itinera.conformance.run.ScenarioReporter
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.apartFromIdAndTime
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind

/**
 * The sentences that read the global metadata each scripted reporter received, and the events that
 * must not carry it.
 */
internal class MetadataSentences(private val world: World) {
    @Then("""^the reporters ("[^"]*"(?:, "[^"]*")*) received the same global metadata$""")
    fun receivedTheSameGlobalMetadata(reporters: String) {
        val received = names(reporters).associateWith { keysAndValuesOf(world, it) }
        holds(
            received.values.toSet().size == 1,
            "the same global metadata for $reporters",
            "$received",
        )
    }

    @Then("no engine event carries the global metadata value of {string}")
    fun noEngineEventCarriesTheValueOf(key: String) {
        val values =
            world.recorders.all.flatMap(ScenarioReporter::entries).filter { entry ->
                isUnder(entry, key)
            }
        val events = world.stream()
        holds(values.isNotEmpty(), "a reporter to receive the global metadata \"$key\"", "none")
        holdsOfStream(
            events.filter(::isEngineEvent).none { event -> carriesAny(event, values) },
            "no engine event to carry the value of \"$key\"",
            events,
        )
    }

    @Then("the reporter {string} received the global metadata:")
    fun receivedTheGlobalMetadata(reporter: String, table: DataTable) {
        val found = entriesOf(world, reporter)
        val expected = table.asMaps()
        holds(
            found.size == expected.size && found.zip(expected).all(::matches),
            "\"$reporter\" to receive $expected",
            "$found",
        )
    }
}

private fun keysAndValuesOf(world: World, name: String): List<Pair<String, Any?>> =
    entriesOf(world, name).map(::keyAndValue)

private fun keyAndValue(entry: Entry): Pair<String, Any?> = entry.key to entry.value

/** Whether [pair]'s entry is what its row says, an empty cell not compared. */
private fun matches(pair: Pair<Entry, Map<String?, String?>>): Boolean {
    val (entry, row) = pair
    return agrees(row["key"], entry.key) &&
        agreesInValue(row["value"], entry.value) &&
        agrees(row["after"], entry.after?.toString() ?: "none")
}

private fun agrees(cell: String?, found: String): Boolean = cell == null || cell == found

private fun agreesInValue(cell: String?, found: Any?): Boolean =
    cell == null || value(cell) == found

/** Whether [event] is one the engine emits, not one a step, hook or provider emits. */
internal fun isEngineEvent(event: Event): Boolean = event.kind !in emittedByCustomCode

private val emittedByCustomCode =
    setOf(
        EventKind.STEP_INFO,
        EventKind.STEP_WARNING,
        EventKind.STEP_ERROR,
        EventKind.JOURNEY_INFO,
        EventKind.JOURNEY_WARNING,
        EventKind.JOURNEY_ERROR,
    )

/** Whether any field of [event] shows the value of one of [entries]. */
private fun carriesAny(event: Event, entries: List<Entry>): Boolean = entries.any { entry ->
    shows(event, entry)
}

private fun shows(event: Event, entry: Entry): Boolean =
    "${entry.value}" in "${apartFromIdAndTime(event)}"

private fun isUnder(entry: Entry, key: String): Boolean = entry.key == key
