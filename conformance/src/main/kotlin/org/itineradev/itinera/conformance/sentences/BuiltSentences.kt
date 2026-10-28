package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.MadeLabeller
import org.itineradev.itinera.conformance.run.Received
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds

/** The sentences that read what each step was built with: its inputs and its services. */
internal class BuiltSentences(private val world: World) {
    @Then("""^step "([^"]*)" was built with input "([^"]*)" = (.+)$""")
    fun wasBuiltWithInput(step: String, key: String, written: String) {
        builtWithInput(step, 1, key, value(written))
    }

    @Then("step {string} was built with input {string} absent")
    fun wasBuiltWithInputAbsent(step: String, key: String) {
        val inputs = received(step, 1).inputs
        holds(
            key in inputs && inputs[key] == null,
            "\"$step\" built with \"$key\" absent",
            "the inputs $inputs",
        )
    }

    @Then("""^step "([^"]*)" was built for attempt (\d+) with input "([^"]*)" = (.+)$""")
    fun wasBuiltForAttemptWithInput(step: String, attempt: Int, key: String, written: String) {
        builtWithInput(step, attempt, key, value(written))
    }

    @Then("step {string} was given the service {string} labelled {string} for attempt {int}")
    fun wasGivenTheService(step: String, key: String, label: String, attempt: Int) {
        val service = received(step, attempt).services[key]
        holds(
            (service as? MadeLabeller)?.labelled == label,
            "\"$step\" given \"$key\" labelled \"$label\" for attempt $attempt",
            "$service",
        )
    }

    private fun builtWithInput(step: String, attempt: Int, key: String, expected: Any?) {
        val inputs = received(step, attempt).inputs
        holds(
            key in inputs && inputs[key] == expected,
            "\"$step\" built for attempt $attempt with \"$key\" = $expected",
            "the inputs $inputs",
        )
    }

    private fun received(step: String, attempt: Int): Received =
        world.witness.built(step).getOrNull(attempt - 1)?.received
            ?: throw Unmet("expected \"$step\" built for attempt $attempt; it was not")
}
