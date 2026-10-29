package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.JourneyEmitted
import org.itineradev.itinera.conformance.model.ProviderAction
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.NotAValue

/** The sentences that script what a service provider does on every call before it returns. */
internal class ProviderActionSentences(private val world: World) {
    private fun does(provider: String, action: ProviderAction) {
        world.model.workflow.provider(provider).actions += action
    }

    @Given("the service provider {string} emits {word} {string}")
    fun emits(provider: String, kind: String, message: String) {
        does(provider, ProviderAction.Emits(JourneyEmitted.named(kind), message, null))
    }

    @Given("the service provider {string} emits {word} {string} with data that is not a value")
    fun emitsANonValue(provider: String, kind: String, message: String) {
        does(provider, ProviderAction.Emits(JourneyEmitted.named(kind), message, NotAValue))
    }

    @Given("""^the service provider "([^"]*)" sets the global metadata "([^"]*)" to (.+)$""")
    fun setsGlobalMetadata(provider: String, key: String, written: String) {
        does(provider, ProviderAction.SetsGlobalMetadata(key, value(written)))
    }
}
