package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.abortsAt
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.JourneyStatus
import org.itineradev.itinera.value.kotlin.typeToken

private val anything = typeToken<Any?>()

/** The sentences that read the result's abort and data bag. */
internal class ResultSentences(private val world: World) {
    @Then("the result's abort carries the error {string}")
    fun abortCarriesTheError(message: String) {
        val found = abort()
        holds(
            found.error != null && found.errorMessage == message,
            "an abort carrying the error \"$message\"",
            "$found",
        )
    }

    @Then("the result's abort carries no error")
    fun abortCarriesNoError() {
        val found = abort()
        holds(found.error == null, "an abort carrying no error", "$found")
    }

    @Then(
        "the result names the step {string} as where the journey was aborted, with the abort reason {string}"
    )
    fun namesTheAbortedStep(step: String, reason: String) {
        val found = abort()
        val events = world.stream()
        holdsOfStream(
            found.reason.toString() == reason && events.any { abortsAt(it, step) },
            "an abort \"$reason\", whose journey_aborted names \"$step\"",
            events,
        )
    }

    private fun abort(): Abort =
        world.result.abort
            ?: throw Unmet(
                "expected the journey to be aborted; it ${statusOf(world.result.status)}"
            )

    @Then("the result's data bag contains:")
    fun dataBagContains(table: DataTable) {
        val bag = dataBag()
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

    @Then("the result's data bag has no key {string}")
    fun dataBagHasNoKey(key: String) {
        val keys = dataBag().keys
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

    private fun dataBag(): DataBag =
        when (val status = world.result.status) {
            is JourneyStatus.Succeeded -> status.dataBag
            is JourneyStatus.Failed -> status.dataBag
            is JourneyStatus.Aborted -> throw Unmet("expected a data bag; the journey was aborted")
        }
}
