package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ProviderRequest
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.step.Attempt

/** The sentences that read how often each service provider was called, and what it received. */
internal class ProvidedSentences(private val world: World) {
    @Then("the service provider {string} was called {int} times")
    fun wasCalled(provider: String, times: Int) {
        val calls = world.witness.calls(provider).size
        holds(calls == times, "\"$provider\" called $times times", "$calls calls")
    }

    @Then("the service provider {string} received the step name {string}")
    fun receivedTheStepName(provider: String, step: String) {
        receivedOnACall(provider, ProviderRequest.StepName, step)
    }

    @Then("the service provider {string} received the attempt number {int}")
    fun receivedTheAttempt(provider: String, attempt: Int) {
        receivedOnACall(provider, ProviderRequest.Attempt, Attempt.of(attempt).toString())
    }

    @Then("the service provider {string} received the journey ID {string}")
    fun receivedTheJourneyId(provider: String, id: String) {
        receivedOnACall(provider, ProviderRequest.JourneyId, id)
    }

    @Then("the service provider {string} received data from the workflow {string} absent")
    fun receivedDataAbsent(provider: String, key: String) {
        val calls = world.witness.calls(provider)
        holds(
            calls.any { received -> isAbsentIn(received, key) },
            "\"$provider\" to receive \"$key\" absent",
            "the calls $calls",
        )
    }

    private fun receivedOnACall(provider: String, request: ProviderRequest, written: String) {
        val calls = world.witness.calls(provider)
        holds(
            calls.any { received -> wasGiven(received, request, written) },
            "\"$provider\" to receive $written for $request",
            "the calls $calls",
        )
    }
}

private fun wasGiven(
    received: Map<ProviderRequest, Any?>,
    request: ProviderRequest,
    written: String,
): Boolean = received[request]?.toString() == written

private fun isAbsentIn(received: Map<ProviderRequest, Any?>, key: String): Boolean =
    received.entries.any { entry -> isAbsentFor(entry, key) }

private fun isAbsentFor(entry: Map.Entry<ProviderRequest, Any?>, key: String): Boolean {
    val request = entry.key
    return request is ProviderRequest.FromWorkflow && request.key == key && entry.value == null
}
