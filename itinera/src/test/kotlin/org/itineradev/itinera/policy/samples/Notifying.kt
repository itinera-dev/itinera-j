@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** A role the shop provides: telling a customer something. */
@Role
fun interface Messages {
    fun tell(message: String)
}

/** A shop that tells its customers through its own channel. */
class Corner(private val told: MutableList<String>) : Messages {
    override fun tell(message: String) {
        told += "corner: $message"
    }
}

/** Tells the customer once the order is paid. */
class Notify {
    fun paid(messages: Messages) {
        messages.tell("your order is paid")
    }
}

fun notify(): WorkflowPolicyDescriptor<Notify, Sync> {
    val messages = Requests.role(Messages::class.java)
    return WorkflowPolicyDescriptor.builder("notify", PolicyFactory(::Notify))
        .onWorkflowSuccess(
            listOf(messages),
            SyncWorkflowHook { policy, got -> policy.paid(got[messages]) },
        )
        .build()
}

fun notifying(): WorkflowDescriptor<Corner, Sync> =
    WorkflowDescriptor.builder(Corner::class.java, "orders")
        .step(StepDescriptor.builder("pay") { SyncStepRun { Outcome.success() } }.build())
        .role(Messages::class.java)
        .policy(notify())
        .build()
