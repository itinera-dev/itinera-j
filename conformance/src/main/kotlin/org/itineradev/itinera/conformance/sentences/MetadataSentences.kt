package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Entry
import org.itineradev.itinera.conformance.run.holds

/** The sentences that read the global metadata each scripted reporter received. */
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
