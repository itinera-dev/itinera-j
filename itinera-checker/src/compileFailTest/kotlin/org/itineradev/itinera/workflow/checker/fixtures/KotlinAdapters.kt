package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.InputAdapter
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Input adapters' mistakes in Kotlin. */
class KotlinAdapters private constructor() {
    @Step
    class Charging(@Input("amount") private val amount: Long) : ItineraStep {
        fun run(): Outcome = if (amount > 0) succeed() else fail("free")
    }

    @Step
    class AsyncCharging : ItineraStep {
        suspend fun run(): Outcome = succeed()
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
        @InputAdapter(steps = ["charge"])
        open fun pricing(asked: T): Supplied = Supplied.FromDataBag
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
}
