package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildingTest {
    @Test
    fun `a step's inputs are resolved before its services`() {
        val called = mutableListOf<String>()
        val order = Requests.input("order", TypeToken.of(String::class.java))
        val gateway = Requests.service("gateway", Runnable::class.java)
        val provider =
            ProviderDescriptor.builder<Shop, Runnable>(
                "payments",
                "gateway",
                Runnable::class.java,
            ) { _, _ ->
                Provided.Instance(Runnable {}).also { called += "called" }
            }
        val charging =
            building("charge") { SyncStepRun { Outcome.success() } }.request(order).request(gateway)
        val result =
            ran(
                instanceOf(shop().step(charging.build()).provider(provider.build()).build())
                    .create()
            )
        assertEquals(
            RequiredDataMissing(StepName.of("charge"), MissingData.Key("order", ByStep)),
            result.abort?.details,
        )
        assertEquals(emptyList<String>(), called)
    }
}
