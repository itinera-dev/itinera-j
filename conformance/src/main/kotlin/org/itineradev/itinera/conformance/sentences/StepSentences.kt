package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import java.util.Collections
import java.util.IdentityHashMap
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.Status
import org.itineradev.itinera.conformance.trace.attemptsOf
import org.itineradev.itinera.conformance.trace.statusOf

/** The sentences that read how each step ended, how often it was built, and whether any ran. */
internal class StepSentences(private val world: World) {
    @Then("step {string} ended as {status} after {int} attempts")
    fun endedAsAfter(step: String, status: Status, attempts: Int) {
        val events = world.stream()
        holdsOfStream(
            statusOf(events, step) == status && attemptsOf(events, step) == attempts,
            "\"$step\" to end as ${status.written} after $attempts attempts",
            events,
        )
    }

    @Then("step {string} ended as aborted")
    fun endedAsAborted(step: String) {
        val events = world.stream()
        holdsOfStream(
            statusOf(events, step) == Status.ABORTED,
            "\"$step\" to end as aborted",
            events,
        )
    }

    @Then("step {string} was built {int} times, each time as a new instance")
    fun wasBuilt(step: String, times: Int) {
        val built = world.witness.built(step)
        val distinct = built.toCollection(Collections.newSetFromMap(IdentityHashMap())).size
        holds(
            built.size == times && distinct == times,
            "\"$step\" built $times times, each a new instance",
            "${built.size} built, $distinct of them distinct",
        )
    }

    @Then("no step ran")
    fun noStepRan() {
        val ran = world.witness.ran
        holds(ran == 0, "no step to run", "$ran steps ran")
    }
}
