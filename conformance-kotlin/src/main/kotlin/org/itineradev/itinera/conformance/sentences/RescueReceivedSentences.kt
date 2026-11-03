package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.Received
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.step.Reason

/** The sentences that read what a rescue step received of the step it rescues. */
internal class RescueReceivedSentences(private val world: World) {
    @Then("""^step "([^"]*)" was built with rescued step data "([^"]*)" = (.+)$""")
    fun wasBuiltWithRescuedStepData(step: String, key: String, written: String) {
        received(step, stepData(key), value(written))
    }

    @Then("step {string} was built with rescued step data {string} absent")
    fun wasBuiltWithRescuedStepDataAbsent(step: String, key: String) {
        received(step, stepData(key), null)
    }

    @Then(
        """^step "([^"]*)" received the failure reason with code "([^"]*)", message "([^"]*)" """ +
            """and details (\{.*\})$"""
    )
    fun receivedTheFailureReason(step: String, code: String, message: String, details: String) {
        received(step, ::isReason, Reason(code, message, value(details)))
    }

    @Then("step {string} received the error with the message {string}")
    fun receivedTheError(step: String, message: String) {
        val error = receivedBy(step, ::isError) as? Throwable
        holds(
            error?.message == message,
            "\"$step\" to receive the error with the message \"$message\"",
            "$error",
        )
    }

    private fun received(step: String, asked: (HookRequest) -> Boolean, expected: Any?) {
        val found = receivedBy(step, asked)
        holds(found == expected, "\"$step\" to receive $expected", "$found")
    }

    /** What the rescue step [step] received for its request that [asked] picks. */
    private fun receivedBy(step: String, asked: (HookRequest) -> Boolean): Any? {
        val received = builtOnce(step).ofRescuedStep
        val request =
            received.keys.firstOrNull(asked)
                ?: throw Unmet("expected \"$step\" to request it; it requested ${received.keys}")
        return received[request]
    }

    private fun builtOnce(step: String): Received<*> =
        world.witness.built(step).firstOrNull()?.received
            ?: throw Unmet("expected \"$step\" built; it was not")
}
