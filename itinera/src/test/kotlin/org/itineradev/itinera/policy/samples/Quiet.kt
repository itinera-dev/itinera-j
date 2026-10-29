package org.itineradev.itinera.policy.samples

import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** A policy whose hooks do nothing of their own. */
class Quiet

fun misattached(): List<Violation> {
    val messages = Requests.role(Messages::class.java)
    val audit =
        StepPolicyDescriptor.builder("audit", PolicyFactory(::Quiet))
            .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
            .build()
    val count =
        StepPolicyDescriptor.builder("count", PolicyFactory(::Quiet))
            .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
            .build()
    val notify =
        WorkflowPolicyDescriptor.builder("notify", PolicyFactory(::Quiet))
            .onWorkflowFailure(
                listOf(messages),
                SyncWorkflowHook { _, got -> got[messages].tell("the order failed") },
            )
            .build()
    val alert =
        WorkflowPolicyDescriptor.builder("alert", PolicyFactory(::Quiet))
            .onWorkflowFailure(SyncWorkflowHook { _, _ -> })
            .build()
    val charge =
        StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
            .policy(audit)
            .policy(count)
            .build()
    return try {
        WorkflowDescriptor.builder(Shop::class.java, "shop")
            .step(charge)
            .policy(notify)
            .policy(alert)
            .build()
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }
}
