package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.abortsAt
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.journey.FailureCause

/** The sentences that read the result's abort and failure. */
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

    @Then(
        "the result names the step {string} whose hook failed the journey, with the code {string}"
    )
    fun namesTheStepWhoseHookFailed(step: String, code: String) {
        val failure = world.result.failure
        val events = world.stream()
        holdsOfStream(
            failure?.cause == FailureCause.FAIL_WORKFLOW &&
                failure.reason?.code == code &&
                events.filterIsInstance<JourneyFailed>().any { failed -> names(failed, step) },
            "FailWorkflow with \"$code\", whose journey_failed names \"$step\"",
            events,
        )
    }
}

private fun names(failed: JourneyFailed, step: String): Boolean = "${failed.step}" == step
