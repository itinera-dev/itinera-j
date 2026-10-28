package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.Line
import org.itineradev.itinera.conformance.trace.apartFromIdAndTime
import org.itineradev.itinera.conformance.trace.areExactly
import org.itineradev.itinera.conformance.trace.expectedLine
import org.itineradev.itinera.conformance.trace.includesInOrder
import org.itineradev.itinera.conformance.trace.lineOf
import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyStarted

/** The sentences that compare the event stream with an event table, and read what it carries. */
internal class EventSentences(private val world: World) {
    @Then("the events include, in order:")
    fun includeInOrder(table: DataTable) {
        compare(table, ::includesInOrder, "to include, in order")
    }

    @Then("the events are exactly:")
    fun areExactlyThese(table: DataTable) {
        compare(table, ::areExactly, "to be exactly")
    }

    private fun compare(table: DataTable, agree: (List<Line>, List<Line>) -> Boolean, how: String) {
        val expected = table.asMaps().map(::expectedLine)
        val lines = world.stream().map(::lineOf)
        holds(agree(lines, expected), "the events $how $expected", "$lines")
    }

    @Then(
        "every event carries the journey ID {string} and the workflow name {string}, with increasing sequence numbers"
    )
    fun everyEventCarries(id: String, workflow: String) {
        val events = world.stream()
        holdsOfStream(
            events.isNotEmpty() &&
                events.all { belongsTo(it, id, workflow) } &&
                events.zipWithNext().all(::inSequence),
            "every event of \"$workflow\" for \"$id\", in increasing sequence",
            events,
        )
    }

    @Then("no event carries the message {string}")
    fun noEventCarriesTheMessage(message: String) {
        val events = world.stream()
        holdsOfStream(
            events.map(::lineOf).none { carries(it, message) },
            "no event to carry \"$message\"",
            events,
        )
    }

    @Then("no contribution of {string} was committed")
    fun noContributionWasCommitted(key: String) {
        val events = world.stream()
        holdsOfStream(
            events.filterIsInstance<ContributionCommitted>().none { commits(it, key) },
            "no contribution_committed of \"$key\"",
            events,
        )
    }

    @Then("no event was emitted")
    fun noEvent() {
        val events = world.stream()
        holdsOfStream(events.isEmpty(), "no event", events)
    }

    @Then("journey_aborted names no adapter")
    fun abortedNamesNoAdapter() {
        val events = world.stream()
        val aborted = events.filterIsInstance<JourneyAborted>().map(::lineOf)
        holdsOfStream(
            aborted.isNotEmpty() && aborted.none(::namesAnAdapter),
            "journey_aborted naming no adapter",
            events,
        )
    }

    @Then("the last event is {string}")
    fun lastEvent(event: String) {
        val events = world.stream()
        holdsOfStream(
            events.lastOrNull()?.kind.toString() == event,
            "the last event to be $event",
            events,
        )
    }

    @Then(
        """^the journey_started event lists the initial keys ("[^"]*"(?:, "[^"]*")*) without their values$"""
    )
    fun listsTheInitialKeys(keys: String) {
        val events = world.stream()
        val started = events.filterIsInstance<JourneyStarted>().firstOrNull()
        holdsOfStream(
            started?.initialKeys == names(keys).toSet(),
            "journey_started to list $keys",
            events,
        )
    }

    @Then(
        "the second journey's events are the first journey's events, apart from the journey ID and timestamps"
    )
    fun secondJourneyRepeatsTheFirst() {
        val (first, second) = world.journeys.map(world::stream).map(::apartFromIdAndTime)
        holds(first == second, "the events $first", "$second")
    }
}

private fun belongsTo(event: Event, id: String, workflow: String): Boolean =
    event.journeyId.toString() == id && event.workflow.toString() == workflow

private fun namesAnAdapter(line: Line): Boolean = "adapter" in line.cells

private fun inSequence(pair: Pair<Event, Event>): Boolean =
    pair.first.sequence < pair.second.sequence

/** Whether [line] carries [message] in a cell or in what no column shows. */
private fun carries(line: Line, message: String): Boolean =
    message in line.cells.values || message in line.carried

private fun commits(event: ContributionCommitted, key: String): Boolean = event.key == key
