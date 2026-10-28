package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Journey
import org.itineradev.itinera.conformance.run.holds

private val uuidV4 =
    Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE,
    )

/** The sentences that check each journey's ID, and whether its instance was created. */
internal class JourneyIdSentences(private val world: World) {
    @Then("the journey ID is a UUID v4")
    fun isAUuid() {
        val found = world.journey.journeyId
        holds(uuidV4.matches(found.orEmpty()), "a UUID v4", "\"$found\"")
    }

    @Then("the journey ID is {string}")
    fun journeyIdIs(id: String) {
        val found = world.journey.journeyId
        holds(found == id, "the journey ID \"$id\"", "\"$found\"")
    }

    @Then("the result's journey ID is {string}")
    fun resultsJourneyIdIs(id: String) {
        val found = world.result.journeyId.toString()
        holds(found == id, "the result's journey ID \"$id\"", "\"$found\"")
    }

    @Then("no journey ID was produced")
    fun noJourneyId() {
        val ids = world.journeys.mapNotNull(Journey::journeyId)
        holds(ids.isEmpty(), "no workflow instance, so no journey ID", "$ids")
    }

    @Then("creating the workflow instance failed")
    fun creatingTheInstanceFailed() {
        val ended = world.journey.ended
        holds(ended is Ended.NotCreated, "creating the workflow instance to fail", "$ended")
    }

    @Then("the two journeys have different journey IDs")
    fun differentJourneyIds() {
        val ids = world.journeys.map(Journey::journeyId)
        holds(ids.size == 2 && ids.toSet().size == 2, "two journeys with different IDs", "$ids")
    }
}
