package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.run.Journey
import org.itineradev.itinera.conformance.run.ScenarioReporter
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.run.isOf
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind

/** The sentences that read the events each scripted reporter received. */
internal class ReceivedSentences(private val world: World) {
    @Then("""^the reporters ("[^"]*"(?:, "[^"]*")*) received the same events$""")
    fun receivedTheSameEvents(reporters: String) {
        val received = names(reporters).associateWith { eventsOf(world, it) }
        holds(received.values.toSet().size == 1, "the same events for $reporters", "$received")
    }

    @Then("the reporter {string} received the event {string}")
    fun receivedTheEvent(reporter: String, event: String) {
        receivedTimes(reporter, event, "at least once", ::isPositive)
    }

    @Then("the reporter {string} received the event {string} {int} times")
    fun receivedTheEventTimes(reporter: String, event: String, times: Int) {
        receivedTimes(reporter, event, "$times times", times::equals)
    }

    @Then("the reporter {string} did not receive the event {string}")
    fun didNotReceive(reporter: String, event: String) {
        receivedTimes(reporter, event, "never", ::isZero)
    }

    private fun receivedTimes(
        reporter: String,
        event: String,
        said: String,
        met: (Int) -> Boolean,
    ) {
        val kind = EventKind.parse(event) ?: throw ModelException("$event is not a kind of event")
        val events = eventsOf(world, reporter)
        holdsOfStream(
            met(events.map(Event::kind).count(kind::equals)),
            "\"$reporter\" to receive $event $said",
            events,
        )
    }

    @Then("the reporter {string} received no event")
    fun receivedNoEvent(reporter: String) {
        val events = eventsOf(world, reporter)
        holdsOfStream(events.isEmpty(), "\"$reporter\" to receive no event", events)
    }

    @Then("each instance's reporter {string} received only its own journey's events")
    fun receivedOnlyItsOwn(reporter: String) {
        val made = madeAs(world, reporter)
        holds(
            made.size == world.journeys.size && made.zip(world.journeys).all(::onlyItsOwn),
            "each \"$reporter\" to receive only its own journey's events",
            "${made.map(ScenarioReporter::events)}",
        )
    }
}

private fun isPositive(count: Int): Boolean = count > 0

private fun isZero(count: Int): Boolean = count == 0

private fun onlyItsOwn(made: Pair<ScenarioReporter, Journey>): Boolean =
    made.first.events.all { isOf(it, made.second.journeyId) }
