@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class ServicesTest {
    @ParameterizedTest
    @FieldSource("served")
    fun `a step is served by the provider attached to it, or else by the one attached to no step`(
        providers: List<ProviderDescriptor<Shop, Sync>>,
        refused: List<Violation>,
    ) {
        val workflow =
            WorkflowDescriptor.builder(Shop::class.java, "shop")
                .step(charging())
                .step(succeeding("refund").build())
        providers.forEach(workflow::provider)
        assertEquals(refused, refusedBy(workflow::build))
    }

    @Test
    fun `a provider of a primitive's wrapper serves a step requesting the primitive`() {
        val count =
            StepDescriptor.builder("count") { error("never built") }
                .request(Requests.service("count", Long::class.java))
                .build()
        val counter =
            ProviderDescriptor.builder<Shop, Long>(
                    "counter",
                    "count",
                    Long::class.javaObjectType,
                ) { _, _ ->
                    Provided.NoInstance
                }
                .build()
        val workflow = WorkflowDescriptor.builder(Shop::class.java, "shop").step(count)
        assertEquals(emptyList<Violation>(), refusedBy(workflow.provider(counter)::build))
    }

    private fun refusedBy(build: () -> Any): List<Violation> =
        try {
            build()
            emptyList()
        } catch (refused: Violations) {
            refused.violations
        }

    /** Takes payments. */
    fun interface Gateway {
        fun take(amount: Long): String
    }

    companion object {
        private val charge = StepName.of("charge")

        @JvmField
        val served =
            listOf(
                argumentSet(
                    "by its own provider, though the one of no step supplies another type",
                    listOf(gateway("labels", String::class.java).build(), attachedGateway()),
                    emptyList<Violation>(),
                ),
                argumentSet(
                    "by the provider of no step, of another type",
                    listOf(gateway("labels", String::class.java).build()),
                    listOf(
                        Violation.WrongServiceType(charge, "payments", ProviderName.of("labels"))
                    ),
                ),
                argumentSet(
                    "by no provider, when the only one serves another step",
                    listOf(gateway("refunds", Gateway::class.java).step("refund").build()),
                    listOf(Violation.ServiceNotProvided(charge, "payments")),
                ),
            )

        private fun attachedGateway(): ProviderDescriptor<Shop, Sync> =
            gateway("gateway", Gateway::class.java).step("charge").build()

        private fun <T : Any> gateway(
            name: String,
            type: Class<T>,
        ): ProviderBuilder<Shop, Sync, JourneyProviderKind<Sync>> =
            ProviderDescriptor.builder<Shop, T>(name, "payments", type) { _, _ ->
                Provided.NoInstance
            }

        private fun charging(): StepDescriptor<Sync> =
            StepDescriptor.builder("charge") { error("never built") }
                .request(Requests.service("payments", Gateway::class.java))
                .build()
    }
}
