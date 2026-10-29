package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.Shop
import org.itineradev.itinera.engine.Journeys.building
import org.itineradev.itinera.engine.Journeys.instanceOf
import org.itineradev.itinera.engine.Journeys.ran
import org.itineradev.itinera.engine.Journeys.shop
import org.itineradev.itinera.event.ByProvider
import org.itineradev.itinera.event.ByProviderForKey
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.GlobalMetadataSet
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.event.ProviderSource
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.ServiceProvided
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.instance.InstanceBuilder
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.service.ProviderReporter
import org.itineradev.itinera.service.SyncProvider
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Sync
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LendingTest {
    /** A service the steps request. */
    fun interface Gateway {
        fun charge(amount: Long): Boolean
    }

    class Card : Gateway {
        override fun charge(amount: Long): Boolean = amount > 0
    }

    private val gateway = Requests.service("gateway", Gateway::class.java)
    private val lent = mutableListOf<Gateway>()
    private val called = mutableListOf<String>()

    @Test
    fun `an instance of journey scope is made once and lent to every step it serves`() {
        val log = Log()
        ran(served(journeyScope { _, _ -> Provided.Instance(Card()) }).reporter(log).create())
        assertEquals(2, lent.size)
        assertSame(lent[0], lent[1])
        assertEquals(
            listOf(StepName.of("charge")),
            log.events.filterIsInstance<ServiceProvided>().map(ServiceProvided::step),
        )
    }

    @Test
    fun `an instance of attempt scope is made for every attempt it serves`() {
        val log = Log()
        ran(served(attemptScope { _, _ -> Provided.Instance(Card()) }).reporter(log).create())
        assertEquals(2, lent.size)
        assertEquals(2, log.events.filterIsInstance<ServiceProvided>().size)
        assertNotSame(lent[0], lent[1])
    }

    @Test
    fun `an instance of journey scope is made again for each journey`() {
        val provider = journeyScope { _, _ -> Provided.Instance(Card()) }.build()
        val descriptor = shop().step(lending("charge").build()).provider(provider).build()
        ran(instanceOf(descriptor).create())
        ran(instanceOf(descriptor).create())
        assertEquals(2, lent.size)
        assertNotSame(lent[0], lent[1])
    }

    @Test
    fun `service_provided names the provider, its implementation and the data it gave`() {
        val log = Log()
        ran(
            served(journeyScope { _, _ -> Provided.Instance(Card(), "visa") })
                .reporter(log)
                .create()
        )
        val provided = log.events.filterIsInstance<ServiceProvided>().single()
        assertEquals(
            listOf("gateway", ProviderName.of("payments"), Card::class.java.name, "visa"),
            listOf(provided.key, provided.provider, provided.implementation, provided.data),
        )
    }

    @Test
    fun `the implementation of an instance with no class of its own is the declared service`() {
        val log = Log()
        val provider = journeyScope { _, _ ->
            Provided.Instance(Gateway { amount -> amount < 100 })
        }
        ran(served(provider).reporter(log).create())
        assertEquals(
            Gateway::class.java.name,
            log.events.filterIsInstance<ServiceProvided>().single().implementation,
        )
    }

    @Test
    fun `a provider of attempt scope is told the step and the attempt`() {
        val step = Requests.stepName()
        val attempt = Requests.attempt()
        val provider = attemptScope { _, got ->
            Provided.Instance<Gateway>(Card()).also {
                called += "${got[step]} ${got[attempt].number}"
            }
        }
        ran(served(provider.request(step).request(attempt)).create())
        assertEquals(listOf("charge 1", "ship 1"), called)
    }

    @Test
    fun `a provider that gives no instance fails, and the step could not be built`() {
        val log = Log()
        val result =
            ran(served(journeyScope { _, _ -> Provided.noInstance() }).reporter(log).create())
        assertEquals(
            listOf(
                "journey_started",
                "attempt_started",
                "service_provider_failed",
                "journey_aborted",
            ),
            log.kinds,
        )
        assertEquals(StepCouldNotBeBuilt(charge, ByProvider(payments), null), result.abort?.details)
    }

    @Test
    fun `a provider that throws fails, and the step could not be built`() {
        val error = IllegalStateException("offline")
        val result = ran(served(journeyScope { _, _ -> throw error }).create())
        assertEquals(
            StepCouldNotBeBuilt(charge, ByProvider(payments), "offline"),
            result.abort?.details,
        )
        assertSame(error, result.abort?.error)
    }

    @Test
    fun `a provider that returns null is an unexpected result`() {
        val result = ran(served(journeyScope { _, _ -> returned(null) }).create())
        assertEquals(
            UnexpectedResult(
                charge,
                ByProviderForKey(payments, "gateway"),
                "the service provider returned null, not what it provides",
            ),
            result.abort?.details,
        )
    }

    @Test
    fun `data a provider gives that is not a value aborts the journey`() {
        val result = ran(served(journeyScope { _, _ -> Provided.Instance(Card(), Any()) }).create())
        val details = result.abort?.details as NotAValue.InEvent
        assertEquals(
            listOf(charge, ByProvider(payments), EventKind.SERVICE_PROVIDED),
            listOf(details.step, details.source, details.eventKind),
        )
    }

    @Test
    fun `data a provider requires from the workflow is read first, naming the provider`() {
        val currency = Requests.fromWorkflow("currency", TypeToken.of(String::class.java))
        val provider = journeyScope { _, got ->
            Provided.Instance<Gateway>(Card()).also { called += got[currency] }
        }
        val result = ran(served(provider.request(currency)).create())
        assertEquals(
            RequiredDataMissing(charge, MissingData.Key("currency", ByProvider(payments))),
            result.abort?.details,
        )
        assertEquals(emptyList<String>(), called)
    }

    @Test
    fun `a provider's events name it and the step being built, and come before service_provided`() {
        val log = Log()
        val reporter = Requests.providerReporter()
        val provider = attemptScope { _, got ->
            got[reporter].info("choosing", "main")
            Provided.Instance(Card())
        }
        ran(served(provider.request(reporter)).reporter(log).create())
        assertEquals(listOf("journey_info", "service_provided"), log.kinds.subList(2, 4))
        val info = log.events[2] as JourneyInfo
        assertEquals(
            listOf(ProviderSource(payments, charge, Attempt.of(1)), "choosing", "main"),
            listOf(info.source, info.message, info.data),
        )
    }

    @Test
    fun `an entry a provider sets reaches the reporters, then global_metadata_set names the provider`() {
        val log = Log()
        val reporter = Requests.providerReporter()
        val provider = journeyScope { _, got ->
            got[reporter].setGlobalMetadata("gateway", "main")
            Provided.Instance(Card())
        }
        ran(served(provider.request(reporter)).reporter(log).create())
        assertEquals("gateway" to "main", log.metadata.last())
        assertEquals(
            ProviderSource(payments, charge, Attempt.of(1)),
            log.events.filterIsInstance<GlobalMetadataSet>().single().source,
        )
    }

    @Test
    fun `a provider's reporter reports nothing once the provider has returned`() {
        val reporter = Requests.providerReporter()
        val kept = mutableListOf<ProviderReporter>()
        val provider = journeyScope { _, got ->
            kept += got[reporter]
            Provided.Instance(Card())
        }
        ran(served(provider.request(reporter)).create())
        assertThrows<IllegalStateException> { kept.single().info("late") }
    }

    @Test
    fun `a reporter failing on a provider's event aborts the journey, though the provider caught it`() {
        val log = Log()
        val reporter = Requests.providerReporter()
        val provider = attemptScope { _, got ->
            runCatching { got[reporter].warning("slow") }
            Provided.Instance(Card())
        }
        val result =
            ran(
                served(provider.request(reporter)).reporter(log).reporter(FailingOnWarning).create()
            )
        assertEquals(ReporterFailed(charge, "full"), result.abort?.details)
        assertEquals(
            listOf("journey_started", "attempt_started", "journey_warning", "journey_aborted"),
            log.kinds,
        )
        assertEquals(emptyList<Gateway>(), lent)
    }

    private fun lending(name: String): StepBuilder<Sync> =
        building(name) { got ->
                lent += got[gateway]
                SyncStepRun { Outcome.success() }
            }
            .request(gateway)

    private fun journeyScope(
        provider: SyncProvider<Shop, JourneyProviderKind<Sync>, Gateway>
    ): ProviderBuilder<Shop, Sync, JourneyProviderKind<Sync>> =
        ProviderDescriptor.builder("payments", "gateway", Gateway::class.java, provider)

    private fun attemptScope(
        provider: SyncProvider<Shop, AttemptProviderKind<Sync>, Gateway>
    ): ProviderBuilder<Shop, Sync, AttemptProviderKind<Sync>> =
        ProviderDescriptor.perAttempt("payments", "gateway", Gateway::class.java, provider)

    /** A journey of two steps, `charge` and `ship`, each lent the gateway [provider] makes. */
    private fun served(provider: ProviderBuilder<Shop, Sync, *>): InstanceBuilder =
        instanceOf(
            shop()
                .step(lending("charge").build())
                .step(lending("ship").build())
                .provider(provider.build())
                .build()
        )

    /** Fails on every `journey_warning`. */
    private object FailingOnWarning : Reporter {
        override fun report(event: Event) {
            check(event.kind != EventKind.JOURNEY_WARNING) { "full" }
        }
    }

    private companion object {
        val charge: StepName = StepName.of("charge")
        val payments: ProviderName = ProviderName.of("payments")
    }
}
