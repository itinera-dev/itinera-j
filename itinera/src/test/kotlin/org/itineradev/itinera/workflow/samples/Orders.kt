@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The shop whose orders the workflow takes. */
class Orders(val shop: String)

fun steps(): StepDescriptor<Sync> =
    StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
        .retryBudget(2)
        .noRetryOnAbnormalTermination()
        .build()

fun building(): WorkflowDescriptor<Orders, Sync> =
    WorkflowDescriptor.builder(Orders::class.java, "orders").step(steps()).build()

fun modes(): Mode {
    val orders = building()
    check(orders.mode == Sync)
    return orders.mode
}

fun ids(): WorkflowDescriptor<Orders, Sync> =
    WorkflowDescriptor.builder(Orders::class.java, "orders")
        .step(steps())
        .idGenerator { orders, data ->
            "${orders.shop}-${data.read("order", TypeToken.of(String::class.java)).orNull()}"
        }
        .build()

fun violations(): List<Violation> =
    try {
        WorkflowDescriptor.builder(Orders::class.java, "orders").step(steps()).step(steps()).build()
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }
