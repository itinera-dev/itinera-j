package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.executor.Refusal
import org.itineradev.itinera.journey.JourneyStatus

/** The sentences that check how the journey ended. */
internal class JourneySentences(private val world: World) {
    @Then("the journey succeeded")
    fun succeeded() {
        endedAs("succeeded")
    }

    @Then("the journey failed")
    fun failed() {
        endedAs("failed")
    }

    @Then("the journey was aborted")
    fun wasAborted() {
        endedAs("aborted")
    }

    private fun endedAs(expected: String) {
        val found = statusOf(world.result.status)
        holds(found == expected, "the journey $expected", "it $found")
    }

    @Then("the journey was aborted with the abort reason {string}")
    fun wasAbortedWith(reason: String) {
        val found = world.result.abort?.reason?.toString() ?: statusOf(world.result.status)
        holds(found == reason, "the journey aborted with \"$reason\"", "\"$found\"")
    }

    @Then("run returned a result instead of throwing")
    fun returnedAResult() {
        val ended = world.journey.ended
        holds(ended is Ended.Returned, "run to return a result", "$ended")
    }

    @Then("run was refused")
    fun wasRefused() {
        refusal()
    }

    @Then("run was refused with the message {string}")
    fun wasRefusedWith(message: String) {
        val found = refusal().message.orEmpty()
        holds(message in found, "a refusal carrying \"$message\"", "\"$found\"")
    }

    private fun refusal(): Refusal =
        when (val ended = world.journey.ended) {
            is Ended.Refused -> ended.refusal
            is Ended.Returned ->
                throw Unmet(
                    "expected run to be refused; the journey ${statusOf(ended.result.status)}"
                )
            is Ended.NotCreated ->
                throw Unmet(
                    "expected run to be refused; no instance was created: ${ended.failure.message}"
                )
        }
}

/** How the journey ended, as the cases name it. */
internal fun statusOf(status: JourneyStatus): String =
    when (status) {
        is JourneyStatus.Succeeded -> "succeeded"
        is JourneyStatus.Failed -> "failed"
        is JourneyStatus.Aborted -> "aborted"
    }
