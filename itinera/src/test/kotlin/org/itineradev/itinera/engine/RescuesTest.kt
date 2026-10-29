package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.engine.Journeys.succeeding
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.samples.misrescued
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class RescuesTest {
    @Test
    fun `the misrescued sample is refused for each of its mistakes`() {
        assertEquals(
            listOf(
                Violation.DuplicateStepName(StepName.of("charge_rescue")),
                Violation.RescuedStepRetried(charge),
                Violation.InputAdapterForUnknownStep(
                    AdapterName.of("credentials"),
                    StepName.of("ship_rescue"),
                ),
            ),
            misrescued(),
        )
    }

    @Test
    fun `a step with a rescue step and no retries is not refused`() {
        assertEquals(emptyList<Violation>(), refusedBy(rescuedShop()))
    }

    @ParameterizedTest
    @FieldSource("attached")
    fun `a rescue step's name can be attached to only while its step has a rescue step`(
        attach: (WorkflowBuilder<Shop, Sync>) -> WorkflowBuilder<Shop, Sync>,
        refused: Violation,
    ) {
        assertEquals(emptyList<Violation>(), refusedBy(attach(rescuedShop())))
        assertEquals(listOf(refused), refusedBy(attach(shop().step(succeeding("charge").build()))))
    }

    @ParameterizedTest
    @FieldSource("adaptedTwice")
    fun `a rescue step's name adapted twice is step adapted twice`(
        adapters: List<InputAdapterDescriptor<Shop, Sync>>,
        named: List<String>,
    ) {
        val workflow = rescuedShop()
        adapters.forEach(workflow::inputAdapter)
        assertEquals(
            listOf(Violation.StepAdaptedTwice(rescue, named.map(AdapterName::of))),
            refusedBy(workflow),
        )
    }

    @ParameterizedTest
    @FieldSource("providedTwice")
    fun `a rescue step's name served twice for one key is service provided twice`(
        providers: List<ProviderDescriptor<Shop, Sync>>,
        named: List<String>,
    ) {
        val workflow = rescuedShop()
        providers.forEach(workflow::provider)
        assertEquals(
            listOf(Violation.ServiceProvidedTwice("payments", rescue, named.map(ProviderName::of))),
            refusedBy(workflow),
        )
    }

    private fun refusedBy(workflow: WorkflowBuilder<Shop, Sync>): List<Violation> =
        try {
            workflow.build()
            emptyList()
        } catch (refused: Violations) {
            refused.violations
        }

    companion object {
        private val charge = StepName.of("charge")
        private val rescue = StepName.of("charge_rescue")

        @JvmField
        val attached =
            listOf(
                argumentSet(
                    "an input adapter",
                    { workflow: WorkflowBuilder<Shop, Sync> ->
                        workflow.inputAdapter(credentials("credentials"))
                    },
                    Violation.InputAdapterForUnknownStep(AdapterName.of("credentials"), rescue),
                ),
                argumentSet(
                    "a service provider",
                    { workflow: WorkflowBuilder<Shop, Sync> ->
                        workflow.provider(backup("backup"))
                    },
                    Violation.ServiceProviderForUnknownStep(ProviderName.of("backup"), rescue),
                ),
            )

        @JvmField
        val adaptedTwice =
            listOf(
                argumentSet(
                    "by two adapters",
                    listOf(credentials("credentials"), credentials("tokens")),
                    listOf("credentials", "tokens"),
                ),
                argumentSet(
                    "by one adapter naming it twice",
                    listOf(credentials("credentials", "charge_rescue")),
                    listOf("credentials", "credentials"),
                ),
            )

        @JvmField
        val providedTwice =
            listOf(
                argumentSet(
                    "by two providers",
                    listOf(backup("backup"), backup("spare")),
                    listOf("backup", "spare"),
                ),
                argumentSet(
                    "by one provider naming it twice",
                    listOf(backup("backup", "charge_rescue")),
                    listOf("backup", "backup"),
                ),
            )

        private fun rescuedShop(): WorkflowBuilder<Shop, Sync> =
            shop()
                .step(
                    succeeding("charge")
                        .rescue(
                            RescueStepDescriptor.builder {
                                    SyncRescueRun { RescueOutcome.success() }
                                }
                                .build()
                        )
                        .build()
                )

        private fun credentials(
            name: String,
            vararg also: String,
        ): InputAdapterDescriptor<Shop, Sync> {
            val adapter =
                InputAdapterDescriptor.builder<Shop>(name, "charge_rescue") { _, _ ->
                    Supplied.FromDataBag
                }
            also.forEach(adapter::step)
            return adapter.build()
        }

        private fun backup(name: String, vararg also: String): ProviderDescriptor<Shop, Sync> {
            val provider =
                ProviderDescriptor.builder<Shop, String>(name, "payments", String::class.java) {
                        _,
                        _ ->
                        Provided.NoInstance
                    }
                    .step("charge_rescue")
            also.forEach(provider::step)
            return provider.build()
        }
    }
}
