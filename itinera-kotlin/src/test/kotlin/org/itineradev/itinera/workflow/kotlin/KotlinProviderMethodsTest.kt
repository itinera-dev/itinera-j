@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.kotlin

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.AttemptProviderDescriptor
import org.itineradev.itinera.service.JourneyProviderDescriptor
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.service.ProviderReporter
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.service.SyncProvider
import org.itineradev.itinera.service.async.AsyncProvider
import org.itineradev.itinera.service.async.AsyncProviderReporter
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.annotated.samples.Pay
import org.itineradev.itinera.workflow.annotated.samples.PaymentGateway
import org.itineradev.itinera.workflow.annotated.samples.Receipts
import org.itineradev.itinera.workflow.annotated.samples.Terminal
import org.itineradev.itinera.workflow.annotated.samples.Terminals
import org.itineradev.itinera.workflow.annotated.samples.misscoped
import org.itineradev.itinera.workflow.annotated.samples.providing
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf
import org.itineradev.itinera.workflow.async.samples.AsyncTerminals
import org.itineradev.itinera.workflow.async.samples.AsyncUnpack
import org.itineradev.itinera.workflow.async.samples.asyncProviding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinProviderMethodsTest {
    @Test
    fun `a Kotlin provider of attempt scope is named after its method and serves its key`() {
        val payments = providing().first() as AttemptProviderDescriptor
        assertEquals(
            listOf(ProviderName.of("payments"), "payments", PaymentGateway::class.java),
            listOf(payments.name, payments.key, payments.type),
        )
        assertEquals(listOf(StepName.of("settle")), payments.steps)
        assertEquals(
            listOf("attempt", "provider reporter"),
            payments.requests.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `a Kotlin provider of journey scope keeps the name and key it is given`() {
        val printer = providing().last() as JourneyProviderDescriptor
        assertEquals(
            listOf(ProviderName.of("printer"), "receipts", Receipts::class.java),
            listOf(printer.name, printer.key, printer.type),
        )
        assertEquals(emptyList<StepName>(), printer.steps)
        assertEquals(listOf("journey ID"), printer.requests.map(Request<*, *>::toString))
    }

    @ParameterizedTest
    @FieldSource("shops")
    fun `a Kotlin provider is called on the workflow object with its answers`(
        shop: String,
        taken: String?,
        data: String?,
    ) {
        val payments = providing().first() as AttemptProviderDescriptor
        val (attempt, reporter) = payments.requests
        val told = mutableListOf<String>()
        val got =
            Answers<AttemptProviderKind<Sync>>(
                mapOf(attempt to Attempt.of(2), reporter to Telling(told))
            )
        val call = payments.call as SyncProvider<Terminals, AttemptProviderKind<Sync>, *>
        val provided = call.provide(Terminals(shop), got)
        assertEquals(taken, takenBy(provided))
        assertEquals(data, (provided as? Provided.Instance)?.data)
        assertEquals(listOf("lending the terminal"), told)
    }

    @Test
    fun `an asynchronous Kotlin provider is awaited`() = runBlocking {
        val gateway = asyncProviding() as JourneyProviderDescriptor
        val told = mutableListOf<String>()
        val got =
            Answers<JourneyProviderKind<Async>>(
                mapOf(gateway.requests.single() to AsyncTelling(told))
            )
        val call = gateway.call as AsyncProvider<AsyncTerminals, JourneyProviderKind<Async>, *>
        assertEquals("corner-40", takenBy(call.provide(AsyncTerminals("corner"), got)))
        assertEquals(listOf("connected"), told)
    }

    @ParameterizedTest
    @FieldSource("journeyScopedRequests")
    fun `a Kotlin provider of journey scope requesting the attempt or the step's name is refused`(
        refused: () -> Violations
    ) {
        assertEquals(
            listOf(Violation.AttemptRequestInJourneyScope(ProviderName.of("receipts"))),
            refused().violations,
        )
    }

    @Test
    fun `an annotated provider attached to a step the workflow does not have is refused`() {
        val refused =
            assertThrows<Violations> { WorkflowDescriptor.of(MisattachedProvider::class.java) }
        assertEquals(
            listOf(
                Violation.ServiceProviderForUnknownStep(
                    ProviderName.of("labels"),
                    StepName.of("ship"),
                )
            ),
            refused.violations,
        )
    }

    @ParameterizedTest
    @FieldSource("otherModes")
    fun `a Kotlin provider of the other mode is refused as not accepted`(
        describe: () -> Any,
        refused: Violation,
    ) {
        assertEquals(listOf(refused), assertThrows<Violations> { describe() }.violations)
    }

    @ParameterizedTest
    @FieldSource("misdeclared")
    fun `every mistake in a Kotlin provider is refused when its workflow is described`(
        describe: () -> Any,
        problem: String,
    ) {
        val refused = assertThrows<Violations> { describe() }
        assertEquals(listOf(problem), refused.violations.map(::problem))
    }

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem

    /** What the gateway [provided] takes for 40, or `null` when it provided no instance. */
    private fun takenBy(provided: Provided<*>): String? =
        when (provided) {
            is Provided.Instance -> (provided.instance as PaymentGateway).take(40)
            Provided.NoInstance -> null
        }

    companion object {
        @JvmField
        val shops =
            listOf(
                argumentSet(
                    "the shop's terminal",
                    "corner",
                    Terminal("corner", Attempt.of(2)).take(40),
                    "corner",
                ),
                argumentSet("no instance for a shop with no name", "", null, null),
            )

        @JvmField
        val journeyScopedRequests =
            listOf(
                argumentSet("the attempt", refusing { misscoped() }),
                argumentSet(
                    "the step's name",
                    refusing {
                        assertThrows<Violations> {
                            WorkflowDescriptor.of(NamingProvider::class.java)
                        }
                    },
                ),
            )

        private fun refusing(refused: () -> Violations) = named("refused", refused)

        @JvmField
        val otherModes =
            listOf(
                argumentSet(
                    "a suspend provider in a synchronous workflow",
                    describing { WorkflowDescriptor.of(SuspendingProvider::class.java) },
                    Violation.ModeNotAccepted("service provider labels", Sync),
                ),
                argumentSet(
                    "a provider that is not suspend in an asynchronous workflow",
                    describing { asyncDescriptorOf(PlainAsyncProvider::class) },
                    Violation.ModeNotAccepted("service provider labels", Async),
                ),
            )

        @JvmField
        val misdeclared =
            listOf(
                argumentSet(
                    "a provider naming a step with an empty name",
                    describing { WorkflowDescriptor.of(EmptyStepProvider::class.java) },
                    "names a step with an empty name",
                ),
                argumentSet(
                    "a provider that returns what is not provided",
                    describing { WorkflowDescriptor.of(StringProvider::class.java) },
                    "returns String, not Provided",
                ),
                argumentSet(
                    "a provider that may return null",
                    describing { WorkflowDescriptor.of(MaybeProvider::class.java) },
                    "returns Provided?, which admits null",
                ),
                argumentSet(
                    "a provider of a type variable",
                    describing { WorkflowDescriptor.of(GenericProvider::class.java) },
                    "returns Provided of no class, which names no service type: write the " +
                        "service's type in Provided",
                ),
                argumentSet(
                    "data from the step",
                    describing { WorkflowDescriptor.of(FromStepProvider::class.java) },
                    "marks charged @FromStep, which a service provider never requests",
                ),
                argumentSet(
                    "a key",
                    describing { WorkflowDescriptor.of(KeyProvider::class.java) },
                    "marks key @Key, which a service provider never requests",
                ),
                argumentSet(
                    "a service",
                    describing { WorkflowDescriptor.of(ServiceProvider::class.java) },
                    "marks payments @Service, which a service provider never requests",
                ),
                argumentSet(
                    "a contributor",
                    describing { WorkflowDescriptor.of(ContributingProvider::class.java) },
                    "requests a contributor, which a service provider never receives",
                ),
                argumentSet(
                    "the asynchronous reporter in a synchronous workflow",
                    describing { WorkflowDescriptor.of(AsyncReporterProvider::class.java) },
                    "requests AsyncProviderReporter, which only an asynchronous provider can",
                ),
                argumentSet(
                    "a parameter that requests nothing",
                    describing { WorkflowDescriptor.of(UnaskingProvider::class.java) },
                    "has a parameter that requests nothing, retries: mark data @FromWorkflow, " +
                        "and request the journey ID, the step's name, the attempt and the " +
                        "provider's reporter by their types",
                ),
            )

        private fun describing(describe: () -> Any) = named("describe", describe)
    }
}

/** A provider reporter that keeps the messages of the events it is given. */
private class Telling(private val told: MutableList<String>) : ProviderReporter {
    override fun info(message: String) {
        told += message
    }

    override fun info(message: String, data: Any?) {
        told += message
    }

    override fun warning(message: String) {
        told += message
    }

    override fun warning(message: String, data: Any?) {
        told += message
    }

    override fun error(message: String) {
        told += message
    }

    override fun error(message: String, data: Any?) {
        told += message
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        told += key
    }
}

/** An asynchronous provider reporter that keeps the messages of the events it is given. */
private class AsyncTelling(private val told: MutableList<String>) : AsyncProviderReporter {
    override suspend fun info(message: String) {
        told += message
    }

    override suspend fun info(message: String, data: Any?) {
        told += message
    }

    override suspend fun warning(message: String) {
        told += message
    }

    override suspend fun warning(message: String, data: Any?) {
        told += message
    }

    override suspend fun error(message: String) {
        told += message
    }

    override suspend fun error(message: String, data: Any?) {
        told += message
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        told += key
    }
}

@WithStep(Pay::class, name = "pay")
class NamingProvider : ItineraWorkflow {
    @Provider fun receipts(step: StepName): Provided<String> = provide("receipt of $step")
}

@WithStep(Pay::class, name = "pay")
class MisattachedProvider : ItineraWorkflow {
    @Provider(steps = ["ship"]) fun labels(): Provided<String> = noInstance()
}

@WithStep(Pay::class, name = "pay")
class SuspendingProvider : ItineraWorkflow {
    @Provider suspend fun labels(): Provided<String> = noInstance()
}

@WithStep(AsyncUnpack::class, name = "unpack")
class PlainAsyncProvider : ItineraAsyncWorkflow {
    @Provider fun labels(): Provided<String> = noInstance()
}

@WithStep(Pay::class, name = "pay")
class EmptyStepProvider : ItineraWorkflow {
    @Provider(steps = [""]) fun labels(): Provided<String> = noInstance()
}

@WithStep(Pay::class, name = "pay")
class StringProvider : ItineraWorkflow {
    @Provider fun labels(journey: JourneyId): String = "label $journey"
}

@WithStep(Pay::class, name = "pay")
class MaybeProvider : ItineraWorkflow {
    @Provider fun labels(journey: JourneyId): Provided<String>? = provide("label $journey")
}

@WithStep(Pay::class, name = "pay")
class GenericProvider : ItineraWorkflow {
    @Provider fun <T : Any> labels(): Provided<T> = noInstance()
}

@WithStep(Pay::class, name = "pay")
class FromStepProvider : ItineraWorkflow {
    @Provider fun labels(@FromStep charged: Long): Provided<String> = provide("$charged")
}

@WithStep(Pay::class, name = "pay")
class KeyProvider : ItineraWorkflow {
    @Provider fun labels(@Key key: String): Provided<String> = provide(key)
}

@WithStep(Pay::class, name = "pay")
class ServiceProvider : ItineraWorkflow {
    @Provider
    fun labels(@Service payments: PaymentGateway): Provided<String> = provide(payments.take(1))
}

@WithStep(Pay::class, name = "pay")
class ContributingProvider : ItineraWorkflow {
    @Provider fun labels(contributor: Contributor): Provided<String> = provide("$contributor")
}

@WithStep(Pay::class, name = "pay")
class AsyncReporterProvider : ItineraWorkflow {
    @Provider fun labels(reporter: AsyncProviderReporter): Provided<String> = provide("$reporter")
}

@WithStep(Pay::class, name = "pay")
class UnaskingProvider : ItineraWorkflow {
    @Provider fun labels(retries: Int): Provided<String> = provide("$retries")
}
