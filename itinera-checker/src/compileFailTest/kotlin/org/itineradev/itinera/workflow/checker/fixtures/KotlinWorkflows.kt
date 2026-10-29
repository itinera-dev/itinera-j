package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.async.AsyncReporter
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Workflow classes' mistakes in Kotlin, in both modes. */
class KotlinWorkflows private constructor() {
    @Step
    class Pack : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @Step
    class AsyncPack : ItineraStep {
        suspend fun run(): Outcome = succeed()
    }

    @Workflow(name = "lenient")
    @WithStep(Pack::class, name = "pack")
    class LenientIds : ItineraWorkflow {
        @IdGenerator fun id(data: DataBag): String? = data.toString()
    }

    @Workflow(name = "waiting")
    @WithStep(Pack::class, name = "pack")
    class WaitingIds : ItineraWorkflow {
        @IdGenerator suspend fun id(): String = javaClass.simpleName
    }

    @Workflow(name = "waiting asynchronously")
    @WithStep(AsyncPack::class, name = "pack")
    class AsyncWaitingIds : ItineraAsyncWorkflow {
        @IdGenerator suspend fun id(): String = javaClass.simpleName
    }

    @WithStep(Pack::class, name = "pack")
    class NumberedIds : ItineraWorkflow {
        @IdGenerator fun id(): Long = javaClass.simpleName.length.toLong()
    }

    @WithStep(Pack::class, name = "pack")
    class HiddenIds : ItineraWorkflow {
        @IdGenerator internal fun id(): String = javaClass.simpleName
    }

    class Quiet : AsyncReporter {
        override suspend fun report(event: Event) = Unit
    }

    class Loud : Reporter {
        override fun report(event: Event) = Unit
    }

    @Workflow(name = "quietly", reporters = [Quiet::class])
    @WithStep(AsyncPack::class, name = "pack")
    class Quietly : ItineraAsyncWorkflow

    @Workflow(name = "loudly", reporters = [Loud::class])
    @WithStep(AsyncPack::class, name = "pack")
    class Loudly : ItineraAsyncWorkflow

    @Workflow(name = "quiet in sync", reporters = [Quiet::class])
    @WithStep(Pack::class, name = "pack")
    class QuietlyInSync : ItineraWorkflow

    @Workflow(name = "")
    @WithStep(Pack::class, name = "")
    @WithStep(Pack::class, name = "pack", retryBudget = -1)
    class Unnamed : ItineraWorkflow

    @WithStep(Pack::class, name = "pack")
    @WithStep(AsyncPack::class, name = "pack")
    object Twice : ItineraWorkflow
}
