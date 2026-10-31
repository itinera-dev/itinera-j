package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Implementation
import org.itineradev.itinera.conformance.model.ProviderFailure
import org.itineradev.itinera.conformance.model.Supplies
import org.itineradev.itinera.conformance.model.value

/** The sentences that script what each service provider does when called. */
internal class ProviderSentences(private val world: World) {
    private fun provider(name: String) = world.model.workflow.provider(name)

    @Given(
        "the service provider {string} labels each instance with the value it received for {string}"
    )
    fun labelsWith(provider: String, key: String) {
        provider(provider).labelledBy = key
    }

    @Given("the service provider {string} fails with {string}")
    fun failsWith(provider: String, message: String) {
        provider(provider).failure = ProviderFailure(message)
    }

    @Given(
        "the service provider {string} fails with {string} when it receives {string} for {string}"
    )
    fun failsWhenItReceives(provider: String, message: String, received: String, key: String) {
        provider(provider).failure = ProviderFailure(message, key, received)
    }

    @Given("the service provider {string} makes instances of {implementation}")
    fun makesInstancesOf(provider: String, implementation: Implementation) {
        provider(provider).implementation = implementation
    }

    @Given("""^the service provider "([^"]*)" supplies the data (.+) with each instance$""")
    fun suppliesTheData(provider: String, written: String) {
        provider(provider).supplies = Supplies.Data(value(written))
    }

    @Given("the service provider {string} supplies data that is not a value with each instance")
    fun suppliesANonValue(provider: String) {
        provider(provider).supplies = Supplies.NonValue
    }

    @Given("the service provider {string} returns nothing")
    fun returnsNothing(provider: String) {
        provider(provider).returnsNothing = true
    }

    @Given("the service provider {string} is asynchronous")
    fun isAsynchronous(provider: String) {
        provider(provider).isAsynchronous = true
    }

    @Given("the services the provider {string} makes fail with {string} when used")
    fun servicesFail(provider: String, message: String) {
        provider(provider).servicesFail = message
    }
}
