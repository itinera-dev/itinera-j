@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.async.AsyncLocalExecutor
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.IdGenerator
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow
import org.itineradev.itinera.workflow.async.asyncDescriptorOf

suspend fun asyncShipping(): JourneyResult {
    /** Packs the parcel. */
    @Step
    class Pack : ItineraStep {
        suspend fun run(): Outcome {
            delay(1)
            return succeed()
        }
    }

    /** Ships parcels from one warehouse, naming each journey after the warehouse. */
    @Workflow(name = "shipping")
    @WithStep(Pack::class, name = "pack", retryBudget = 2)
    class AsyncShipping(private val warehouse: String) : ItineraAsyncWorkflow {
        @IdGenerator fun parcelId(): String = "$warehouse-1"
    }

    return AsyncLocalExecutor().run(AsyncShipping("north").instance().create())
}

/** Takes returned parcels back. */
@Workflow(name = "returns")
@WithStep(AsyncUnpack::class, name = "unpack")
class AsyncReturns : ItineraAsyncWorkflow

/** Unpacks a returned parcel. */
@Step
class AsyncUnpack : ItineraStep {
    suspend fun run(): Outcome = succeed()
}

fun asyncDescribing(): WorkflowDescriptor<AsyncReturns, Async> =
    asyncDescriptorOf(AsyncReturns::class)
