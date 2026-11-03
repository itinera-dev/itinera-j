package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.holds

/** The sentences that read the step, attempt, journey, cause and error each hook received. */
internal class HookReceivedFactSentences(private val world: World) {
    @Then("the hook {string} of policy {string} received the step name {string}")
    fun receivedTheStepName(hook: String, policy: String, step: String) {
        world.receivedWritten(hook, policy, ::isStepName, step)
    }

    @Then("the hook {string} of policy {string} received the attempt number {int}")
    fun receivedTheAttempt(hook: String, policy: String, attempt: Int) {
        world.receivedWritten(hook, policy, ::isAttempt, "$attempt")
    }

    @Then("the hook {string} of policy {string} received the journey ID {string}")
    fun receivedTheJourneyId(hook: String, policy: String, id: String) {
        world.receivedWritten(hook, policy, ::isJourneyId, id)
    }

    @Then("the hook {string} of policy {string} received the cause {string}")
    fun receivedTheCause(hook: String, policy: String, cause: String) {
        world.receivedWritten(hook, policy, ::isCause, cause)
    }

    @Then("the hook {string} of policy {string} received an error")
    fun receivedAnError(hook: String, policy: String) {
        val errors = world.receivedFor(hook, policy, ::isError)
        holds(
            errors.any(::isThrowable),
            "\"$hook\" of \"$policy\" to receive an error",
            "$errors",
        )
    }

    @Then("the hook {string} of policy {string} received the error with the message {string}")
    fun receivedTheErrorWithTheMessage(hook: String, policy: String, message: String) {
        val messages = world.receivedFor(hook, policy, ::isError).map(::messageOf)
        holds(
            message in messages,
            "\"$hook\" of \"$policy\" to receive an error \"$message\"",
            "$messages",
        )
    }
}

private fun isThrowable(value: Any?): Boolean = value is Throwable

private fun messageOf(value: Any?): String? = (value as? Throwable)?.message
