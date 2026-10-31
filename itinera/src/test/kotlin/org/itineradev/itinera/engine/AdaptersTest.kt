@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.samples.misadapted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class AdaptersTest {
    @Test
    fun `an adapter's shared name, a step it adapts twice and a step it names in vain are each refused`() {
        assertEquals(
            listOf(
                Violation.DuplicateAdapterName(pricing),
                Violation.StepAdaptedTwice(charge, listOf(pricing, pricing)),
                Violation.InputAdapterForUnknownStep(pricing, StepName.of("ship")),
            ),
            misadapted(),
        )
    }

    @ParameterizedTest
    @FieldSource("misattached")
    fun `every misattached input adapter is refused when the workflow is built`(
        adapters: List<InputAdapterDescriptor<Shop, Sync>>,
        refused: Violation,
    ) {
        val workflow = shop()
        adapters.forEach(workflow::inputAdapter)
        assertEquals(listOf(refused), assertThrows<Violations> { workflow.build() }.violations)
    }

    @Test
    fun `one adapter may serve several steps`() {
        val adapter = adapter("pricing", "charge").step("refund").build()
        assertEquals(listOf(adapter), shop().inputAdapter(adapter).build().inputAdapters)
    }

    @Test
    fun `an adapter and a service provider may have the same name`() {
        val workflow =
            shop()
                .inputAdapter(adapter("pricing", "charge").build())
                .provider(provider("pricing"))
                .build()
        assertEquals(
            listOf(pricing) to listOf(ProviderName.of("pricing")),
            workflow.inputAdapters.map(InputAdapterDescriptor<Shop, Sync>::name) to
                workflow.providers.map(ProviderDescriptor<Shop, Sync>::name),
        )
    }

    companion object {
        private val pricing = AdapterName.of("pricing")
        private val charge = StepName.of("charge")

        @JvmField
        val misattached =
            listOf(
                argumentSet(
                    "two adapters on one step",
                    listOf(
                        adapter("pricing", "charge").build(),
                        adapter("discounts", "charge").build(),
                    ),
                    Violation.StepAdaptedTwice(
                        charge,
                        listOf(pricing, AdapterName.of("discounts")),
                    ),
                ),
                argumentSet(
                    "one adapter naming a step twice",
                    listOf(adapter("pricing", "charge").step("charge").build()),
                    Violation.StepAdaptedTwice(charge, listOf(pricing, pricing)),
                ),
                argumentSet(
                    "two adapters with one name, on different steps",
                    listOf(
                        adapter("pricing", "charge").build(),
                        adapter("pricing", "refund").build(),
                    ),
                    Violation.DuplicateAdapterName(pricing),
                ),
                argumentSet(
                    "an adapter on a step the workflow does not have",
                    listOf(adapter("pricing", "ship").build()),
                    Violation.InputAdapterForUnknownStep(pricing, StepName.of("ship")),
                ),
            )

        private fun adapter(name: String, step: String): InputAdapterBuilder<Shop, Sync> =
            InputAdapterDescriptor.builder(name, step) { _, _ -> Supplied.FromDataBag }

        private fun provider(name: String): ProviderDescriptor<Shop, Sync> =
            ProviderDescriptor.builder<Shop, String>(name, "labels", String::class.java) { _, _ ->
                    Provided.Instance("label")
                }
                .build()

        private fun shop(): WorkflowBuilder<Shop, Sync> =
            WorkflowDescriptor.builder(Shop::class.java, "shop")
                .step(succeeding("charge").build())
                .step(succeeding("refund").build())
    }
}
