package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Call
import org.itineradev.itinera.conformance.run.RanAlone
import org.itineradev.itinera.conformance.run.SETTING
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.value.TypeToken

/** The sentences that read what test support's recorders kept of a step or a hook run alone. */
internal class KeptSentences(private val world: World) {
    @Then("the contributions recorder holds:")
    fun contributionsHold(table: DataTable) {
        val contributions = world.ranAlone.contributions
        val expected = table.asMaps().map(::entryOf)
        for ((key, value) in expected) {
            val read = contributions.read(key, ANY).orNull()
            holds(read == value, "\"$key\" = $value in the contributions recorder", "$read")
        }
        val keys = contributions.keys
        holds(keys == expected.map(::keyOf).toSet(), "the keys of $expected", "$keys")
    }

    @Then("""^the (step|hook) event reporter kept the events:$""")
    fun keptTheEvents(recorder: String, table: DataTable) {
        val kept = keptBy(recorder).emitted.map(::written)
        val expected = table.asMaps().map(::expectedEmitted)
        holds(kept == expected, "the $recorder event reporter to keep $expected", "$kept")
    }

    @Then("""^the (step|hook) event reporter kept nothing$""")
    fun keptNothing(recorder: String) {
        val ran = keptBy(recorder)
        holds(
            ran.emitted.isEmpty() && ran.globalMetadata.isEmpty(),
            "the $recorder event reporter to keep nothing",
            "${ran.emitted} and ${ran.globalMetadata}",
        )
    }

    @Then("the step event reporter kept the global metadata:")
    fun keptTheGlobalMetadata(table: DataTable) {
        val kept = keptBy("step").globalMetadata.toList()
        val expected = table.asMaps().map(::entryOf)
        holds(kept == expected, "the global metadata $expected", "$kept")
    }

    @Then(
        """^every call (step "[^"]*"|the hook "[^"]*" of policy "[^"]*") made to its """ +
            """(?:step|hook) reporter was interrupted$"""
    )
    fun everyCallInterrupted(called: String) {
        val calls = calledAlone(called).calls
        holds(
            calls.isNotEmpty() && calls.all(Call::interrupted),
            "every call to the reporter to be interrupted",
            "$calls",
        )
    }

    @Then(
        "the call step {string} made setting {string}, and every call after it, was interrupted, and no call before it"
    )
    fun interruptedFromSetting(step: String, key: String) {
        val calls = calledAlone("step \"$step\"").calls
        val setting = calls.indexOfFirst { call -> isSetting(call, key) }
        if (setting < 0) throw ModelException("step \"$step\" never set \"$key\"")
        holds(
            calls.take(setting).none(Call::interrupted) &&
                calls.drop(setting).all(Call::interrupted),
            "the call setting \"$key\", and every call after it, to be interrupted, and no call before it",
            "$calls",
        )
    }

    private fun keptBy(recorder: String): RanAlone {
        val ran = world.ranAlone
        if ("${ran.recorder}" != recorder) {
            throw ModelException(
                "the $recorder event reporter lent nothing; ${ran.called} ran alone"
            )
        }
        return ran
    }

    private fun calledAlone(called: String): RanAlone {
        val ran = world.ranAlone
        if (ran.called != called) throw ModelException("$called did not run alone")
        return ran
    }
}

private val ANY = TypeToken.of(Any::class.java)

private fun keyOf(entry: Pair<String, Any?>): String = entry.first

private fun isSetting(call: Call, key: String): Boolean = call.method == SETTING && call.key == key

/** An event a recorder kept, as a table writes it: its level, its message and its data. */
private data class WrittenEmitted(val level: String, val message: String, val data: Any?)

private fun written(emitted: Emitted): WrittenEmitted =
    WrittenEmitted(emitted.level.name.lowercase(), emitted.message, emitted.data)

private fun expectedEmitted(row: Map<String?, String?>): WrittenEmitted =
    WrittenEmitted(cell(row, "level"), cell(row, "message"), dataOf(row["data"]))

private fun dataOf(written: String?): Any? =
    if (written == null || written == "none") null else value(written)
