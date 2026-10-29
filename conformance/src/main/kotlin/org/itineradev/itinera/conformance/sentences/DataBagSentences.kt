package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.apartFromIdAndTime
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.value.kotlin.typeToken

private val anything = typeToken<Any?>()

/** The sentences that read the result's data bag, and what the events show of it. */
internal class DataBagSentences(private val world: World) {
    @Then("the result's data bag contains:")
    fun dataBagContains(table: DataTable) {
        contains(dataBag(world.result), table)
    }

    @Then("the second journey's data bag contains:")
    fun secondDataBagContains(table: DataTable) {
        contains(dataBag(secondResult()), table)
    }

    private fun secondResult(): JourneyResult =
        (world.journeys.getOrNull(1)?.ended as? Ended.Returned)?.result
            ?: throw Unmet("expected a second journey with a result; found ${world.journeys}")

    @Then("the result's data bag has no key {string}")
    fun dataBagHasNoKey(key: String) {
        val keys = dataBag(world.result).keys
        holds(key !in keys, "no \"$key\" in the data bag", "$keys")
    }

    @Then("the result carries no data bag")
    fun carriesNoDataBag() {
        val status = world.result.status
        holds(
            status is JourneyStatus.Aborted,
            "no data bag",
            "the data bag of a journey that ${statusOf(status)}",
        )
    }

    @Then("no engine event carries the value of {string}")
    fun noEngineEventCarriesTheValueOf(key: String) {
        val shown = "${dataBag(world.result).read(key, anything).orNull()}"
        val events = world.stream()
        holdsOfStream(
            events.filter(::isEngineEvent).none { event -> shows(event, shown) },
            "no engine event to carry $shown, the value of \"$key\"",
            events,
        )
    }
}

private fun contains(bag: DataBag, table: DataTable) {
    for (row in table.asMaps()) {
        val key = cell(row, "key")
        val expected = value(cell(row, "value"))
        holds(
            key in bag.keys && bag.read(key, anything).orNull() == expected,
            "\"$key\" = $expected in the data bag",
            "${bag.read(key, anything).orNull()}",
        )
    }
}

private fun dataBag(result: JourneyResult): DataBag =
    when (val status = result.status) {
        is JourneyStatus.Succeeded -> status.dataBag
        is JourneyStatus.Failed -> status.dataBag
        is JourneyStatus.Aborted -> throw Unmet("expected a data bag; the journey was aborted")
    }

private fun shows(event: Event, shown: String): Boolean = shown in "${apartFromIdAndTime(event)}"
