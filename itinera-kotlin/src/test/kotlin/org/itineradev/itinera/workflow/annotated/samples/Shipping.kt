@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

fun shipping(): JourneyResult {
    /** Packs the parcel. */
    class Pack : ItineraStep {
        fun run(): Outcome = succeed()
    }

    /** Ships the parcel, trying twice more if the carrier fails. */
    class Ship : ItineraStep {
        fun run(): Outcome = succeed()
    }

    /** Ships parcels from one warehouse, naming each journey after the warehouse and the parcel. */
    @Workflow(name = "shipping")
    @WithStep(Pack::class, name = "pack")
    @WithStep(Ship::class, name = "ship", retryBudget = 2)
    class Shipping(private val warehouse: String) : ItineraWorkflow {
        @IdGenerator
        fun parcelId(data: DataBag): String =
            "$warehouse-${data.read("parcel", TypeToken.of(String::class.java)).orNull()}"
    }

    return LocalExecutor().run(Shipping("north").instance().data("parcel", "7").create())
}

/** Takes returned parcels back. */
@Workflow(name = "returns")
@WithStep(Unpack::class, name = "unpack")
class Returns : ItineraWorkflow

/** Unpacks a returned parcel. */
class Unpack : ItineraStep {
    fun run(): Outcome = succeed()
}

fun describing(): WorkflowDescriptor<Returns, Sync> = WorkflowDescriptor.of(Returns::class.java)
