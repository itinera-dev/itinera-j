@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

private val CARD = Requests.input("card", TypeToken.of(String::class.java))
private val DECLINED = Requests.optionalReason()
private val CONTRIBUTOR = Requests.contributor()

/**
 * The charge, whose rescue step renews the card after a failed attempt, before it is attempted
 * again, unless the card was stolen.
 */
fun rescues(): StepDescriptor<Sync> {
    val renewal =
        RescueStepDescriptor.builder { got ->
                val card = got[CARD]
                val declined = got[DECLINED]?.code ?: "unknown"
                val contributor = got[CONTRIBUTOR]
                SyncRescueRun {
                    if (declined == "stolen") {
                        RescueOutcome.failure("stolen card")
                    } else {
                        contributor.contribute("card", "$card renewed after $declined")
                        RescueOutcome.success()
                    }
                }
            }
            .request(CARD)
            .request(DECLINED)
            .request(CONTRIBUTOR)
            .build()
    return StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
        .rescue(renewal)
        .build()
}

fun misrescued(): List<Violation> {
    val retried =
        StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
            .rescue(
                RescueStepDescriptor.builder { SyncRescueRun { RescueOutcome.success() } }.build()
            )
            .retryBudget(1)
            .build()
    val credentials =
        InputAdapterDescriptor.builder<Orders>("credentials", "ship_rescue") { _, _ ->
                Supplied.FromDataBag
            }
            .build()
    return try {
        WorkflowDescriptor.builder(Orders::class.java, "orders")
            .step(retried)
            .step(
                StepDescriptor.builder("charge_rescue") { SyncStepRun { Outcome.success() } }
                    .build()
            )
            .step(StepDescriptor.builder("ship") { SyncStepRun { Outcome.success() } }.build())
            .inputAdapter(credentials)
            .build()
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }
}
