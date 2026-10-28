@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.JourneyProviderDescriptor
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.service.SyncProvider
import org.itineradev.itinera.service.samples.misprovided
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class ProvidersTest {
    @Test
    fun `a provider added twice, and one for a step the workflow lacks, are each refused`() {
        assertEquals(
            listOf(
                Violation.WrongServiceType(StepName.of("charge"), "payments", gateway),
                Violation.DuplicateProviderName(gateway),
                Violation.ServiceProvidedTwice("payments", null, listOf(gateway, gateway)),
                Violation.ServiceProviderForUnknownStep(
                    ProviderName.of("courier"),
                    StepName.of("ship"),
                ),
            ),
            misprovided(),
        )
    }

    @ParameterizedTest
    @FieldSource("misattached")
    fun `every misattached service provider is refused when the workflow is built`(
        providers: List<ProviderDescriptor<Shop, Sync>>,
        refused: Violation,
    ) {
        val workflow = shop()
        providers.forEach(workflow::provider)
        assertEquals(listOf(refused), assertThrows<Violations> { workflow.build() }.violations)
    }

    @Test
    fun `one provider may serve several steps`() {
        val provider = provider("gateway").step("charge").step("refund").build()
        assertEquals(listOf(provider), shop().provider(provider).build().providers)
    }

    @Test
    fun `a hand-written provider of journey scope requesting the attempt is refused`() {
        val provider =
            object : JourneyProviderDescriptor<Shop, Sync> {
                override val name: ProviderName = gateway
                override val key: String = "payments"
                override val type: Class<*> = String::class.java
                override val steps: List<StepName> = emptyList()
                override val requests: List<Request<JourneyProviderKind<Sync>, *>> =
                    listOf(returned(Requests.attempt()), returned(Requests.stepName()))
                override val call: ProviderCall<Sync, Shop, JourneyProviderKind<Sync>, *> =
                    SyncProvider<Shop, JourneyProviderKind<Sync>, String> { _, _ ->
                        Provided.NoInstance
                    }
            }
        assertEquals(
            listOf(Violation.AttemptRequestInJourneyScope(gateway)),
            assertThrows<Violations> { shop().provider(provider).build() }.violations,
        )
    }

    companion object {
        private val gateway = ProviderName.of("gateway")

        @JvmField
        val misattached =
            listOf(
                argumentSet(
                    "two providers on one step",
                    listOf(
                        provider("gateway").step("charge").build(),
                        provider("backup").step("charge").build(),
                    ),
                    Violation.ServiceProvidedTwice(
                        "payments",
                        StepName.of("charge"),
                        listOf(gateway, ProviderName.of("backup")),
                    ),
                ),
                argumentSet(
                    "one provider naming a step twice",
                    listOf(provider("gateway").step("charge").step("charge").build()),
                    Violation.ServiceProvidedTwice(
                        "payments",
                        StepName.of("charge"),
                        listOf(gateway, gateway),
                    ),
                ),
                argumentSet(
                    "two providers of one key on no step",
                    listOf(provider("gateway").build(), provider("backup").build()),
                    Violation.ServiceProvidedTwice(
                        "payments",
                        null,
                        listOf(gateway, ProviderName.of("backup")),
                    ),
                ),
                argumentSet(
                    "two providers with one name, of different keys",
                    listOf(
                        provider("gateway").build(),
                        ProviderDescriptor.builder<Shop, String>(
                                "gateway",
                                "labels",
                                String::class.java,
                            ) { _, _ ->
                                Provided.NoInstance
                            }
                            .build(),
                    ),
                    Violation.DuplicateProviderName(gateway),
                ),
                argumentSet(
                    "a provider on a step the workflow does not have",
                    listOf(provider("gateway").step("ship").step("ship").build()),
                    Violation.ServiceProviderForUnknownStep(gateway, StepName.of("ship")),
                ),
            )

        private fun provider(name: String): ProviderBuilder<Shop, Sync, JourneyProviderKind<Sync>> =
            ProviderDescriptor.builder<Shop, String>(name, "payments", String::class.java) { _, _ ->
                Provided.NoInstance
            }

        private fun shop(): WorkflowBuilder<Shop, Sync> =
            WorkflowDescriptor.builder(Shop::class.java, "shop")
                .step(succeeding("charge").build())
                .step(succeeding("refund").build())
    }
}
