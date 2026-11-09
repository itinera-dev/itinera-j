package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.AdapterAnswer
import org.itineradev.itinera.conformance.model.ProviderScope
import org.itineradev.itinera.conformance.model.ServiceType
import org.itineradev.itinera.conformance.model.value

/**
 * The sentences that define input adapters and service providers before the workflow, and bind them
 * to it after.
 */
internal class BindingSentences(private val world: World) {
    private val blocks
        get() = world.model.blocks

    @Given("""^the input adapter "([^"]*)" returns (.+) when step "([^"]*)" requests "([^"]*)"$""")
    fun returnsWhenStepRequests(adapter: String, written: String, step: String, key: String) {
        blocks.definedAdapter(adapter).answers(key, answerOf(written), step)
    }

    @Given("the input adapter {string} fails with {string} when step {string} requests {string}")
    fun failsWhenStepRequests(adapter: String, message: String, step: String, key: String) {
        blocks.definedAdapter(adapter).answers(key, AdapterAnswer.Fails(message), step)
    }

    @Given("the workflow binds the input adapter {string} to the step {string}")
    fun bindsTheInputAdapterToTheStep(adapter: String, step: String) {
        blocks.bindAdapter(adapter, listOf(step))
    }

    @Given(
        """^the workflow binds the input adapter "([^"]*)" to the steps ("[^"]*"(?:, "[^"]*")*)$"""
    )
    fun bindsTheInputAdapterToTheSteps(adapter: String, steps: String) {
        blocks.bindAdapter(adapter, names(steps))
    }

    @Given("""^the service provider "([^"]*)" makes an? (\w+) for "([^"]*)", for each attempt$""")
    fun makesForEachAttempt(provider: String, type: String, key: String) {
        blocks.defineProvider(provider, key, ServiceType.named(type), ProviderScope.ATTEMPT)
    }

    @Given("""^the service provider "([^"]*)" makes an? (\w+) for "([^"]*)", once per journey$""")
    fun makesOncePerJourney(provider: String, type: String, key: String) {
        blocks.defineProvider(provider, key, ServiceType.named(type), ProviderScope.JOURNEY)
    }

    @Given("the workflow binds the service provider {string}")
    fun bindsTheServiceProvider(provider: String) {
        blocks.bindProvider(provider, null)
    }

    @Given("the workflow binds the service provider {string} to the step {string}")
    fun bindsTheServiceProviderToTheStep(provider: String, step: String) {
        blocks.bindProvider(provider, listOf(step))
    }

    @Given(
        """^the workflow binds the service provider "([^"]*)" to the steps ("[^"]*"(?:, "[^"]*")*)$"""
    )
    fun bindsTheServiceProviderToTheSteps(provider: String, steps: String) {
        blocks.bindProvider(provider, names(steps))
    }
}

private fun answerOf(written: String): AdapterAnswer =
    if (written == "nothing") AdapterAnswer.Nothing else AdapterAnswer.Returns(value(written))
