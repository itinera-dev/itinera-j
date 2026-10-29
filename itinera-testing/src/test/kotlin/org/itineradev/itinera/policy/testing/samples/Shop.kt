@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.testing.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.testing.HookEnding
import org.itineradev.itinera.policy.testing.StepPolicyScript
import org.itineradev.itinera.policy.testing.WorkflowPolicyScript
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.testing.Ending
import org.itineradev.itinera.step.testing.StepMock
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The shop whose orders the workflows take. */
class Shop

fun endings(): List<HookEnding> =
    listOf(
        HookEnding.keepDefault(),
        HookEnding.finishWorkflow(),
        HookEnding.failWorkflow(Reason("fraud", "The card is reported stolen.")),
        HookEnding.failWorkflow("fraud"),
        HookEnding.fails("the ledger is down"),
    )

fun onSuccess(): OnSuccess = HookEnding.finishWorkflow().onSuccess()

fun onFailure(): OnFailure = HookEnding.failWorkflow("fraud").onFailure()

fun onWorkflow(): String? =
    try {
        HookEnding.fails("the ledger is down").played()
        null
    } catch (error: RuntimeException) {
        error.message
    }

fun stepPolicies(): JourneyResult {
    val contributor = Requests.contributor()
    val audit =
        StepPolicyDescriptor.builder(
                "audit",
                StepPolicyScript.keepingDefault().onStepFailure(HookEnding.failWorkflow("fraud")),
            )
            .onStepFailure(
                listOf(contributor),
                SyncHook { mock, got ->
                    got[contributor].contribute("audited", true)
                    mock.onStepFailure()
                },
            )
            .build()
    val result =
        ran(
            StepDescriptor.builder("charge", StepMock.playing(Ending.failure("declined")))
                .policy(audit)
        )
    check(result.failure?.reason?.code == "fraud")
    return result
}

fun workflowPolicies(): JourneyResult {
    val ledger =
        WorkflowPolicyDescriptor.builder(
                "ledger",
                WorkflowPolicyScript.returning().onWorkflowSuccess(HookEnding.fails("down")),
            )
            .onWorkflowSuccess(SyncWorkflowHook { mock, _ -> mock.onWorkflowSuccess() })
            .build()
    val orders =
        WorkflowDescriptor.builder(Shop::class.java, "orders")
            .step(StepDescriptor.builder("charge", StepMock.playing(Ending.success())).build())
            .policy(ledger)
            .build()
    val result = LocalExecutor().run(orders.instance(Shop()).create())
    check(result.abort?.errorMessage == "down")
    return result
}

fun failingToBuild(): AbortReason? {
    val audit =
        StepPolicyDescriptor.builder(
                "audit",
                StepPolicyScript.keepingDefault().failingToBuild("no ledger"),
            )
            .onStepSuccess(SyncHook { mock, _ -> mock.onStepSuccess() })
            .build()
    val result =
        ran(StepDescriptor.builder("charge", StepMock.playing(Ending.success())).policy(audit))
    check(result.abort?.errorMessage == "no ledger")
    return result.abort?.reason
}

private fun ran(charge: StepBuilder<Sync>): JourneyResult {
    val orders = WorkflowDescriptor.builder(Shop::class.java, "orders").step(charge.build()).build()
    return LocalExecutor().run(orders.instance(Shop()).create())
}
