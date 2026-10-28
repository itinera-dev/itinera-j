package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ProviderScope
import org.itineradev.itinera.conformance.model.RequestedService
import org.itineradev.itinera.conformance.model.ServiceType

/**
 * The sentences that declare the input adapters, the services each step requests, the service
 * providers and the steps each serves.
 */
internal class ServiceSentences(private val world: World) {
    private val model
        get() = world.model

    @Given(
        """^the workflow declares the input adapter "([^"]*)" for the steps ("[^"]*"(?:, "[^"]*")*)$"""
    )
    fun declaresTheInputAdapter(adapter: String, steps: String) {
        model.workflow.adapt(adapter, names(steps))
    }

    @Given("""^step "([^"]*)" requests the service "([^"]*)" of type (\w+)$""")
    fun requestsTheService(step: String, key: String, type: String) {
        model.step(step).services += RequestedService(key, ServiceType.named(type))
    }

    @Given(
        """^the workflow declares the service provider "([^"]*)" for "([^"]*)" of type (\w+), once per journey$"""
    )
    fun declaresAJourneyProvider(provider: String, key: String, type: String) {
        model.workflow.provide(provider, key, ServiceType.named(type), ProviderScope.JOURNEY)
    }

    @Given(
        """^the workflow declares the service provider "([^"]*)" for "([^"]*)" of type (\w+), for each attempt$"""
    )
    fun declaresAnAttemptProvider(provider: String, key: String, type: String) {
        model.workflow.provide(provider, key, ServiceType.named(type), ProviderScope.ATTEMPT)
    }

    @Given("""^the service provider "([^"]*)" serves only the steps ("[^"]*"(?:, "[^"]*")*)$""")
    fun servesOnly(provider: String, steps: String) {
        model.workflow.provider(provider).servesOnly(names(steps))
    }
}
