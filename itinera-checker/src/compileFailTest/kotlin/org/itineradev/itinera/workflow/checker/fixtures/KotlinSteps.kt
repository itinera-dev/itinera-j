package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Step classes' mistakes in Kotlin. */
class KotlinSteps private constructor() {
    @Step
    class Pausing : ItineraStep {
        suspend fun run(): Outcome = succeed()
    }

    @WithStep(Pausing::class, name = "pack") class Suspending : ItineraWorkflow

    @Step
    class Waiting : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @WithStep(Waiting::class, name = "wait") class Blocking : ItineraAsyncWorkflow

    @Step
    class Lenient : ItineraStep {
        fun run(): Outcome? = succeed()
    }

    @WithStep(Lenient::class, name = "pack") class NullableOutcome : ItineraWorkflow

    @Step
    object Alone : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @WithStep(Alone::class, name = "alone") class Singleton : ItineraWorkflow

    @Step
    class Defaulted(@Input("note") private val note: String = "none") : ItineraStep {
        fun run(): Outcome = if (note.isEmpty()) fail("empty") else succeed()
    }

    @WithStep(Defaulted::class, name = "note") class DefaultArguments : ItineraWorkflow

    @Step
    class Overloaded
    @JvmOverloads
    constructor(
        @Input("note") private val note: String,
        @Input("count") private val count: Long = 1,
    ) : ItineraStep {
        fun run(): Outcome = if (note.length > count) succeed() else fail("short")
    }

    @WithStep(Overloaded::class, name = "note") class Overloads : ItineraWorkflow

    @Step
    class Hidden internal constructor() : ItineraStep {
        internal fun run(): Outcome = succeed()
    }

    @WithStep(Hidden::class, name = "hidden") class Internal : ItineraWorkflow

    @Step
    class Talking : ItineraStep {
        fun run(): String = javaClass.simpleName
    }

    @WithStep(Talking::class, name = "talk") class RunReturnsString : ItineraWorkflow

    @Step
    open class Base : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @Step class Derived : Base()

    @WithStep(Derived::class, name = "derived") class Inherited : ItineraWorkflow

    @Step
    class Untyped(@Input("anything") private val anything: Any?) : ItineraStep {
        fun run(): Outcome = if (anything == null) fail("none") else succeed()
    }

    @Step
    inner class Inner : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @WithStep(Untyped::class, name = "untyped")
    @WithStep(Inner::class, name = "inner")
    class Mixed : ItineraWorkflow
}
