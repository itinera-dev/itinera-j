@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.kotlin.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.kotlin.builder

/** The shop whose orders the workflow takes. */
class Shop(val name: String)

fun reifiedBuilding(): WorkflowDescriptor<Shop, Sync> =
    builder<Shop>("orders")
        .step(StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }.build())
        .idGenerator { shop, _ -> "${shop.name}-1" }
        .build()
