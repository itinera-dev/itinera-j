@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.async.samples

import kotlinx.coroutines.delay
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncStep

/** The shop whose orders the workflow takes. */
class AsyncOrders(val shop: String)

fun asyncBuilding(): WorkflowDescriptor<AsyncOrders, Async> =
    asyncBuilder<AsyncOrders>("orders")
        .step(
            asyncStep("charge") {
                    AsyncStepRun {
                        delay(1)
                        Outcome.success()
                    }
                }
                .retryBudget(2)
                .build()
        )
        .idGenerator { orders, _ -> "${orders.shop}-1" }
        .build()
