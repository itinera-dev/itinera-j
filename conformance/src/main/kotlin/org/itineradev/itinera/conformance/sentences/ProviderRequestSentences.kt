package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ProviderRequest
import org.itineradev.itinera.conformance.model.ValueType

/** The sentences that script what each service provider requests, in the order written. */
internal class ProviderRequestSentences(private val world: World) {
    private fun provider(name: String) = world.model.workflow.provider(name)

    @Given("the service provider {string} requests the step name")
    fun requestsTheStepName(provider: String) {
        provider(provider).requests += ProviderRequest.StepName
    }

    @Given("the service provider {string} requests the attempt number")
    fun requestsTheAttempt(provider: String) {
        provider(provider).requests += ProviderRequest.Attempt
    }

    @Given("the service provider {string} requests the journey ID")
    fun requestsTheJourneyId(provider: String) {
        provider(provider).requests += ProviderRequest.JourneyId
    }

    @Given("the service provider {string} requests data from the workflow {string} of type {word}")
    fun requestsData(provider: String, key: String, type: String) {
        provider(provider).requests +=
            ProviderRequest.FromWorkflow(key, ValueType.named(type), optional = false)
    }

    @Given(
        "the service provider {string} requests optional data from the workflow {string} of type {word}"
    )
    fun requestsOptionalData(provider: String, key: String, type: String) {
        provider(provider).requests +=
            ProviderRequest.FromWorkflow(key, ValueType.named(type), optional = true)
    }
}
