@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.playing
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.event.ByRescueStep
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.OptionalInputAbsent
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.StepSource
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepBuilder
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

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

    @Test
    fun `a rescue step receives what the failed attempt contributed, and its reason`() {
        val account = Requests.fromStep("account", TypeToken.of(String::class.java))
        val reason = Requests.reason()
        val received = mutableListOf<Any?>()
        val charging =
            contributing(
                "charge",
                { contributor ->
                    contributor.contribute("account", "A-1")
                    Outcome.failure(expired)
                },
                { Outcome.success() },
            )
        ran(
            charging,
            receiving(received) { got -> listOf(got[account], got[reason]) }
                .request(account)
                .request(reason),
        )
        assertEquals(listOf("A-1", expired), received)
    }

    @Test
    fun `a rescue step receives its own copy of the error that ended the attempt`() {
        val error = Requests.error()
        val received = mutableListOf<Any?>()
        val unauthorised = IllegalStateException("unauthorised")
        ran(
            playing("charge", unauthorised, null),
            receiving(received) { got -> listOf(got[error]) }.request(error),
        )
        val copy = received.single() as Throwable
        assertEquals("unauthorised", copy.message)
        assertNotSame(unauthorised, copy)
    }

    @Test
    fun `a rescue step's required reason that the attempt did not leave aborts the journey, naming it`() {
        val result =
            ran(
                playing("charge", IllegalStateException("down")),
                renewing().request(Requests.reason()),
            )
        assertEquals(
            RequiredDataMissing(rescue, MissingData.FailureReason(ByRescueStep)),
            result.abort?.details,
        )
    }

    @Test
    fun `optional data the failed attempt did not contribute is absent, naming the rescue step`() {
        val log = Log()
        val account = Requests.optionalFromStep("account", TypeToken.of(String::class.java))
        val received = mutableListOf<Any?>()
        val result =
            ran(
                declinedOnce(),
                receiving(received) { got -> listOf(got[account]) }.request(account),
                log,
            )
        assertNull(received.single())
        assertEquals(
            OptionalInputAbsent(
                6,
                Journeys.clock.instant(),
                result.journeyId,
                log.events[5].workflow,
                "account",
                StepSource(rescue, Attempt.of(1)),
            ),
            log.events[5],
        )
    }

    @ParameterizedTest
    @FieldSource("adapted")
    fun `a rescue step's inputs come through the adapter attached to its own name, never its step's`(
        adapters: List<InputAdapterDescriptor<Shop, Sync>>,
        input: String,
    ) {
        val card = Requests.input("card", TypeToken.of(String::class.java))
        val received = mutableListOf<Any?>()
        val reading = receiving(received) { got -> listOf(got[card]) }.request(card).build()
        val workflow = shop().step(declinedOnce().rescue(reading).build())
        adapters.forEach(workflow::inputAdapter)
        ran(instanceOf(workflow.build()).data("card", "kept").create())
        assertEquals(listOf(input), received)
    }

    @ParameterizedTest
    @FieldSource("served")
    fun `a rescue step's service comes from the provider attached to its own name, never its step's`(
        providers: List<ProviderDescriptor<Shop, Sync>>,
        service: String,
    ) {
        val wallet = Requests.service("wallet", String::class.java)
        val received = mutableListOf<Any?>()
        val serving = receiving(received) { got -> listOf(got[wallet]) }.request(wallet).build()
        val workflow = shop().step(declinedOnce().rescue(serving).build())
        providers.forEach(workflow::provider)
        ran(instanceOf(workflow.build()).create())
        assertEquals(listOf(service), received)
    }

    private fun ran(
        charging: StepBuilder<Sync>,
        rescue: RescueStepBuilder<Sync>,
        log: Log = Log(),
    ): JourneyResult =
        ran(
            instanceOf(shop().step(charging.rescue(rescue.build()).build()).build())
                .reporter(log)
                .create()
        )

    companion object {
        private val rescue = StepName.of("charge_rescue")
        private val expired = Reason("expired", "The token expired.", mapOf("since" to 3L))
        private val wallet = supplying("wallet", "charged", "charge")

        @JvmField
        val adapted =
            listOf(
                argumentSet(
                    "with only its step's adapter, from the data bag",
                    listOf(wallet),
                    "kept",
                ),
                argumentSet(
                    "with an adapter of its own",
                    listOf(wallet, supplying("cards", "renewed", "charge_rescue")),
                    "renewed",
                ),
            )

        @JvmField
        val served =
            listOf(
                argumentSet(
                    "with its step's provider, from the one attached to no step",
                    listOf(
                        lending("shared", "every step"),
                        lending("charging", "charge", "charge"),
                    ),
                    "every step",
                ),
                argumentSet(
                    "with a provider of its own",
                    listOf(
                        lending("shared", "every step"),
                        lending("charging", "charge", "charge"),
                        lending("renewing", "renewal", "charge_rescue"),
                    ),
                    "renewal",
                ),
            )

        private fun declinedOnce(): StepBuilder<Sync> =
            contributing("charge", { Outcome.failure("declined") }, { Outcome.success() })

        private fun renewing(): RescueStepBuilder<Sync> = RescueStepDescriptor.builder {
            SyncRescueRun { RescueOutcome.success() }
        }

        /** A rescue step that adds what [read] reads from what it got to [received]. */
        private fun receiving(
            received: MutableList<Any?>,
            read: (Got<RescueKind<Sync>>) -> List<Any?>,
        ): RescueStepBuilder<Sync> = RescueStepDescriptor.builder { got ->
            received.addAll(read(got))
            SyncRescueRun { RescueOutcome.success() }
        }

        /** A provider named [name] lending [instance] as the wallet, attached to [steps]. */
        private fun lending(
            name: String,
            instance: String,
            vararg steps: String,
        ): ProviderDescriptor<Shop, Sync> {
            val builder =
                ProviderDescriptor.builder<Shop, String>(name, "wallet", String::class.java) { _, _
                    ->
                    Provided.Instance(instance)
                }
            steps.forEach(builder::step)
            return builder.build()
        }

        private fun supplying(
            name: String,
            value: String,
            step: String,
        ): InputAdapterDescriptor<Shop, Sync> =
            InputAdapterDescriptor.builder<Shop>(name, step) { _, _ -> Supplied.Value(value) }
                .build()
    }
}
