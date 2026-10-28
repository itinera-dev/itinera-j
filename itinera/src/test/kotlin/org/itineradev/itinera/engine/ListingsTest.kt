@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.samples.PaymentGateway
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ListedRescue
import org.itineradev.itinera.workflow.ListedService
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.Position
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.samples.listing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListingsTest {
    @Test
    fun `a workflow is listed in order, each step with its policies, adapter, services and rescue section`() {
        val payments = listOf(served("payments", "gateway"))
        assertEquals(
            listOf(
                ListedStep(
                    StepName.of("charge"),
                    Position.of(1),
                    listOf(PolicyName.of("audit"), PolicyName.of("count")),
                    AdapterName.of("pricing"),
                    payments,
                    ListedRescue(StepName.of("charge_rescue"), null, payments),
                ),
                ListedStep(
                    StepName.of("ship"),
                    Position.of(2),
                    emptyList(),
                    null,
                    emptyList(),
                    null,
                ),
            ),
            listing(),
        )
    }

    @Test
    fun `a workflow is listed without building or calling any of its parts`() {
        val listed =
            shop()
                .step(unbuilt("charge").policy(unbuiltPolicy()).rescue(unbuiltRescue()).build())
                .inputAdapter(unadapted("pricing", "charge_rescue"))
                .provider(unprovided("gateway").build())
                .build()
                .listing()
        assertEquals(
            ListedStep(
                StepName.of("charge"),
                Position.of(1),
                listOf(PolicyName.of("audit")),
                null,
                emptyList(),
                ListedRescue(
                    StepName.of("charge_rescue"),
                    AdapterName.of("pricing"),
                    listOf(served("payments", "gateway")),
                ),
            ),
            listed.single(),
        )
    }

    @Test
    fun `a rescue step's services are listed with the provider attached to its name, never its step's`() {
        val listed =
            shop()
                .step(unbuilt("charge").request(gateway).rescue(unbuiltRescue()).build())
                .provider(unprovided("gateway").step("charge").build())
                .provider(unprovided("backup").step("charge_rescue").build())
                .build()
                .listing()
        assertEquals(
            listOf(served("payments", "gateway")) to listOf(served("payments", "backup")),
            listed.single().services to listed.single().rescue?.services,
        )
    }

    @Test
    fun `each service is listed with the provider attached to its step, or else the one attached to no step`() {
        val listed =
            shop()
                .step(unbuilt("charge").request(gateway).request(labels).build())
                .step(unbuilt("refund").request(gateway).build())
                .provider(unprovided("gateway").build())
                .provider(unprovided("charging").step("charge").build())
                .provider(unprovided("labeller", "labels", String::class.java).build())
                .build()
                .listing()
        assertEquals(
            listOf(
                listOf(served("payments", "charging"), served("labels", "labeller")),
                listOf(served("payments", "gateway")),
            ),
            listed.map(ListedStep::services),
        )
    }

    private fun served(key: String, provider: String): ListedService =
        ListedService(key, ProviderName.of(provider))

    companion object {
        private val gateway = Requests.service("payments", PaymentGateway::class.java)
        private val labels = Requests.service("labels", String::class.java)

        private fun unbuilt(name: String): StepBuilder<Sync> =
            StepDescriptor.builder(name) { error("never built") }

        private fun unbuiltPolicy(): StepPolicyDescriptor<Any, Sync> =
            StepPolicyDescriptor.builder("audit", PolicyFactory<Any> { error("never built") })
                .onStepSuccess(SyncHook { _, _ -> error("never called") })
                .build()

        private fun unbuiltRescue(): RescueStepDescriptor<Sync> =
            RescueStepDescriptor.builder { error("never built") }.request(gateway).build()

        private fun unadapted(name: String, step: String): InputAdapterDescriptor<Shop, Sync> =
            InputAdapterDescriptor.builder<Shop>(name, step) { _, _ -> error("never called") }
                .build()

        private fun unprovided(
            name: String
        ): ProviderBuilder<Shop, Sync, JourneyProviderKind<Sync>> =
            unprovided(name, "payments", PaymentGateway::class.java)

        private fun <T : Any> unprovided(
            name: String,
            key: String,
            type: Class<T>,
        ): ProviderBuilder<Shop, Sync, JourneyProviderKind<Sync>> =
            ProviderDescriptor.builder<Shop, T>(name, key, type) { _, _ -> error("never called") }
    }
}
