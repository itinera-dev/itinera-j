@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.kotlin

import kotlinx.coroutines.runBlocking
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.value.kotlin.bagOf
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.SyncInputAdapter
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.annotated.samples.Priced
import org.itineradev.itinera.workflow.annotated.samples.pricing
import org.itineradev.itinera.workflow.async.AsyncInputAdapter
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf
import org.itineradev.itinera.workflow.async.samples.AsyncPriced
import org.itineradev.itinera.workflow.async.samples.asyncPricing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class KotlinAdapterMethodsTest {
    @Test
    fun `a Kotlin input adapter is named after its method and requests what its parameters name`() {
        val pricing = pricing()
        assertEquals(AdapterName.of("pricing"), pricing.name)
        assertEquals(listOf(StepName.of("pay")), pricing.steps)
        assertEquals(
            listOf("key", "data from the workflow price: Long", "data bag access"),
            pricing.requests.map(Request<*, *>::toString),
        )
    }

    @ParameterizedTest
    @FieldSource("supplies")
    fun `a Kotlin input adapter is called on the workflow object with its answers`(
        key: String,
        supplied: Supplied,
    ) {
        val pricing = pricing()
        val (asked, price, access) = pricing.requests
        val got =
            Answers<InputAdapterKind>(
                mapOf(
                    asked to key,
                    price to 40L,
                    access to DataBags.access(bagOf("discount", 5L)),
                )
            )
        assertEquals(supplied, (pricing.call as SyncInputAdapter).adapt(Priced(), got))
    }

    @Test
    fun `an asynchronous Kotlin input adapter keeps the name it is given and is awaited`() =
        runBlocking {
            val prices = asyncPricing()
            val got = Answers<InputAdapterKind>(mapOf(prices.requests.single() to "amount"))
            assertEquals(AdapterName.of("prices"), prices.name)
            assertEquals(
                Supplied.Value(60L),
                (prices.call as AsyncInputAdapter).adapt(AsyncPriced("corner"), got),
            )
        }

    @Test
    fun `an input adapter is read from the workflow class's superclasses too`() {
        val adapter = WorkflowDescriptor.of(Inheriting::class.java).inputAdapters.single()
        assertEquals(AdapterName.of("named"), adapter.name)
        assertEquals(
            listOf("step name", "journey ID"),
            adapter.requests.map(Request<*, *>::toString),
        )
    }

    @Test
    fun `an annotated input adapter attached to a step the workflow does not have is refused`() {
        val refused = assertThrows<Violations> { WorkflowDescriptor.of(Misattached::class.java) }
        assertEquals(
            listOf(
                Violation.InputAdapterForUnknownStep(AdapterName.of("ship"), StepName.of("ship"))
            ),
            refused.violations,
        )
    }

    @Test
    fun `an internal Kotlin input adapter is named as Kotlin writes its method`() {
        assertEquals(
            listOf(AdapterName.of("pricing")),
            WorkflowDescriptor.of(InternalAdapter::class.java)
                .inputAdapters
                .map(InputAdapterDescriptor<*, *>::name),
        )
    }

    @Test
    fun `annotated input adapters are read in the order of their methods' names`() {
        assertEquals(
            listOf(AdapterName.of("discounts"), AdapterName.of("pricing")),
            WorkflowDescriptor.of(TwoAdapters::class.java)
                .inputAdapters
                .map(InputAdapterDescriptor<*, *>::name),
        )
    }

    @Test
    fun `a method overriding an annotated input adapter replaces it`() {
        assertEquals(
            listOf(StepName.of("refund")),
            WorkflowDescriptor.of(Overriding::class.java).inputAdapters.single().steps,
        )
    }

    @ParameterizedTest
    @FieldSource("unmarkedOverrides")
    fun `a method overriding an annotated input adapter without its mark leaves no adapter`(
        describe: () -> WorkflowDescriptor<*, *>
    ) {
        assertEquals(emptyList<InputAdapterDescriptor<*, *>>(), describe().inputAdapters)
    }

    @Test
    fun `annotated input adapters sharing a name and a step are refused for both`() {
        val refused = assertThrows<Violations> { WorkflowDescriptor.of(SharedAdapters::class.java) }
        assertEquals(
            listOf(
                Violation.DuplicateAdapterName(AdapterName.of("pricing")),
                Violation.StepAdaptedTwice(
                    StepName.of("charge"),
                    listOf(AdapterName.of("pricing"), AdapterName.of("pricing")),
                ),
            ),
            refused.violations,
        )
    }

    @Test
    fun `an annotated input adapter naming a step twice is attached to it twice`() {
        val refused = assertThrows<Violations> { WorkflowDescriptor.of(TwiceNamed::class.java) }
        assertEquals(
            listOf(
                Violation.StepAdaptedTwice(
                    StepName.of("charge"),
                    listOf(AdapterName.of("pricing"), AdapterName.of("pricing")),
                )
            ),
            refused.violations,
        )
    }

    @Test
    fun `every mistake of one input adapter is refused at once`() {
        val refused = assertThrows<Violations> { WorkflowDescriptor.of(TwoMistakes::class.java) }
        assertEquals(2, refused.violations.size)
        val (mode, returns) = refused.violations
        assertEquals(Violation.ModeNotAccepted("input adapter pricing", Sync), mode)
        assertEquals("returns String, not Supplied", problem(returns))
    }

    @ParameterizedTest
    @FieldSource("otherModes")
    fun `a Kotlin input adapter of the other mode is refused as not accepted`(
        describe: () -> Any,
        refused: Violation,
    ) {
        assertEquals(listOf(refused), assertThrows<Violations> { describe() }.violations)
    }

    @ParameterizedTest
    @FieldSource("misdeclared")
    fun `every mistake in a Kotlin input adapter is refused when its workflow is described`(
        describe: () -> Any,
        problem: String,
    ) {
        val refused = assertThrows<Violations> { describe() }
        assertEquals(listOf(problem), refused.violations.map(::problem))
    }

    private fun problem(violation: Violation): String = (violation as Violation.Misdeclared).problem

    companion object {
        @JvmField
        val unmarkedOverrides =
            listOf(
                argumentSet(
                    "of the same types",
                    { WorkflowDescriptor.of(UnmarkedOverride::class.java) },
                ),
                argumentSet(
                    "of narrower types",
                    { WorkflowDescriptor.of(NarrowedOverride::class.java) },
                ),
            )

        @JvmField
        val supplies =
            listOf(
                argumentSet("the amount, less the discount", "amount", Supplied.Value(35L)),
                argumentSet("any other input, from the data bag", "currency", Supplied.FromDataBag),
            )

        @JvmField
        val otherModes =
            listOf(
                argumentSet(
                    "a suspend adapter in a synchronous workflow",
                    describing { WorkflowDescriptor.of(SuspendingAdapter::class.java) },
                    Violation.ModeNotAccepted("input adapter pricing", Sync),
                ),
                argumentSet(
                    "an adapter that is not suspend in an asynchronous workflow",
                    describing { asyncDescriptorOf(PlainAsyncAdapter::class) },
                    Violation.ModeNotAccepted("input adapter pricing", Async),
                ),
            )

        @JvmField
        val misdeclared =
            listOf(
                argumentSet(
                    "an adapter naming a step with an empty name",
                    describing { WorkflowDescriptor.of(EmptyStepAdapter::class.java) },
                    "names a step with an empty name",
                ),
                argumentSet(
                    "an adapter attached to no step",
                    describing { WorkflowDescriptor.of(UnattachedAdapter::class.java) },
                    "is attached to no step",
                ),
                argumentSet(
                    "an adapter that returns what is not supplied",
                    describing { WorkflowDescriptor.of(StringAdapter::class.java) },
                    "returns String, not Supplied",
                ),
                argumentSet(
                    "an adapter that may return null",
                    describing { WorkflowDescriptor.of(MaybeAdapter::class.java) },
                    "returns Supplied?, which admits null",
                ),
                argumentSet(
                    "a key that is not a String",
                    describing { WorkflowDescriptor.of(NumberedKey::class.java) },
                    "marks key @Key, though the key is a String",
                ),
                argumentSet(
                    "a key that admits null",
                    describing { WorkflowDescriptor.of(MaybeKey::class.java) },
                    "requests the key as String?, though the key is always given",
                ),
                argumentSet(
                    "data from the step",
                    describing { WorkflowDescriptor.of(FromStepAdapter::class.java) },
                    "marks charged @FromStep, which an input adapter never requests",
                ),
                argumentSet(
                    "a service",
                    describing { WorkflowDescriptor.of(ServiceAdapter::class.java) },
                    "marks payments @Service, which an input adapter never requests",
                ),
                argumentSet(
                    "a contributor",
                    describing { WorkflowDescriptor.of(ContributingAdapter::class.java) },
                    "requests a contributor, which an input adapter never receives",
                ),
                argumentSet(
                    "a parameter that requests nothing",
                    describing { WorkflowDescriptor.of(UnaskingAdapter::class.java) },
                    "has a parameter that requests nothing, retries: mark data @FromWorkflow " +
                        "and the key @Key, and request the step's name, the journey ID and the " +
                        "data bag's access by their types",
                ),
            )

        private fun describing(describe: () -> Any) = named("describe", describe)
    }
}

/** Names the input after the step and the journey. */
open class Naming {
    @InputAdapter(steps = ["charge"])
    fun named(step: StepName, journey: JourneyId): Supplied = Supplied.Value("$journey $step")
}

@WithStep(Charging::class, name = "charge") class Inheriting : Naming(), ItineraWorkflow

@WithStep(Charging::class, name = "charge")
class InternalAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) internal fun pricing(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
@WithStep(Charging::class, name = "refund")
class TwoAdapters : ItineraWorkflow {
    @InputAdapter(steps = ["refund"]) fun pricing(): Supplied = fromDataBag()

    @InputAdapter(steps = ["charge"]) fun discounts(): Supplied = fromDataBag()
}

/** Prices the charge, unless a subclass prices something else. */
open class Pricing {
    @InputAdapter(steps = ["charge"]) open fun pricing(): Supplied = Supplied.FromDataBag
}

@WithStep(Charging::class, name = "charge")
@WithStep(Charging::class, name = "refund")
class Overriding : Pricing(), ItineraWorkflow {
    @InputAdapter(steps = ["refund"]) override fun pricing(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class UnmarkedOverride : Pricing(), ItineraWorkflow {
    override fun pricing(): Supplied = fromDataBag()
}

/** Prices the charge from what it asks for, unless a subclass narrows what that is. */
open class Asking<T> {
    @InputAdapter(steps = ["charge"]) open fun pricing(asked: T): Supplied = Supplied.FromDataBag
}

@WithStep(Charging::class, name = "charge")
class NarrowedOverride : Asking<StepName>(), ItineraWorkflow {
    override fun pricing(asked: StepName): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class SharedAdapters : ItineraWorkflow {
    @InputAdapter(steps = ["charge"], name = "pricing") fun byAmount(): Supplied = fromDataBag()

    @InputAdapter(steps = ["charge"], name = "pricing") fun byWeight(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class TwiceNamed : ItineraWorkflow {
    @InputAdapter(steps = ["charge", "charge"]) fun pricing(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class TwoMistakes : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) suspend fun pricing(@Key key: String): String = key
}

@WithStep(Charging::class, name = "charge")
class Misattached : ItineraWorkflow {
    @InputAdapter(steps = ["ship"]) fun ship(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class SuspendingAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) suspend fun pricing(): Supplied = fromDataBag()
}

@WithStep(AsyncCharging::class, name = "charge")
class PlainAsyncAdapter : ItineraAsyncWorkflow {
    @InputAdapter(steps = ["charge"]) fun pricing(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class EmptyStepAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge", ""]) fun pricing(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class UnattachedAdapter : ItineraWorkflow {
    @InputAdapter(steps = []) fun pricing(): Supplied = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class StringAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) fun pricing(@Key key: String): String = key
}

@WithStep(Charging::class, name = "charge")
class MaybeAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) fun pricing(): Supplied? = fromDataBag()
}

@WithStep(Charging::class, name = "charge")
class NumberedKey : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) fun pricing(@Key key: Int): Supplied = supply(key)
}

@WithStep(Charging::class, name = "charge")
class MaybeKey : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) fun pricing(@Key key: String?): Supplied = supply(key)
}

@WithStep(Charging::class, name = "charge")
class FromStepAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"])
    fun pricing(@FromStep charged: Long): Supplied = supply(charged)
}

@WithStep(Charging::class, name = "charge")
class ServiceAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"])
    fun pricing(@Service payments: String): Supplied = supply(payments)
}

@WithStep(Charging::class, name = "charge")
class ContributingAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"])
    fun pricing(contributor: Contributor): Supplied {
        contributor.contribute("priced", true)
        return fromDataBag()
    }
}

@WithStep(Charging::class, name = "charge")
class UnaskingAdapter : ItineraWorkflow {
    @InputAdapter(steps = ["charge"]) fun pricing(retries: Int): Supplied = supply(retries)
}
