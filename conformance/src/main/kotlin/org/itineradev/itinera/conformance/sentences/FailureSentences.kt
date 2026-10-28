package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.journey.Failure

/** The sentences that read the result's failure. */
internal class FailureSentences(private val world: World) {
    @Then("the result's failure reason has the code {string}")
    fun failureReasonHasTheCode(code: String) {
        val found = failure()
        holds(found.reason?.code == code, "a failure whose reason has the code \"$code\"", "$found")
    }

    @Then("the result's failure has the cause {string}")
    fun failureHasTheCause(cause: String) {
        val found = failure().cause.toString()
        holds(found == cause, "a failure with the cause \"$cause\"", "\"$found\"")
    }

    @Then("the result's failure carries the error {string}")
    fun failureCarriesTheError(message: String) {
        val found = failure().errorMessage
        holds(found == message, "a failure carrying the error \"$message\"", "$found")
    }

    @Then("the result's failure carries no error")
    fun failureCarriesNoError() {
        val found = failure().error
        holds(found == null, "a failure carrying no error", "$found")
    }

    @Then("the result's failure carries no reason")
    fun failureCarriesNoReason() {
        val found = failure().reason
        holds(found == null, "a failure carrying no reason", "$found")
    }

    @Then("the result names the failed step {string} with the code {string}")
    fun namesTheFailedStep(step: String, code: String) {
        val found = failure()
        val events = world.stream()
        holdsOfStream(
            found.reason?.code == code && events.any { failsAt(it, step) },
            "a failure with the code \"$code\", whose journey_failed names \"$step\"",
            events,
        )
    }

    private fun failure(): Failure =
        world.result.failure
            ?: throw Unmet("expected the journey to fail; it ${statusOf(world.result.status)}")
}

private fun failsAt(event: Event, step: String): Boolean =
    event is JourneyFailed && event.step.toString() == step
