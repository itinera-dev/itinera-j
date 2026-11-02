package org.itineradev.itinera.workflow.annotated.samples

import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnWorkflowFailure
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** A role a shop provides: telling a customer something. */
@Role
fun interface Customers {
    fun tell(message: String)
}

/** Tells the customer once their order is paid, or when it could not be. */
@WorkflowPolicy(name = "tell")
class Tell : ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    fun paid(@Role customers: Customers, @FromWorkflow order: String) {
        customers.tell("your order $order is paid")
    }

    @OnWorkflowFailure
    fun unpaid(@Role customers: Customers, @FromWorkflow order: String) {
        customers.tell("your order $order could not be paid")
    }
}

/** Takes the payment. */
@Step
class Pay : ItineraStep {
    fun run(): Outcome = succeed()
}

/** A shop that tells its customers through its own channel, once their order is paid. */
@Workflow(name = "orders", policies = [Tell::class])
@WithStep(Pay::class, name = "pay")
class Corner(private val told: MutableList<String>) : ItineraWorkflow, Customers {
    override fun tell(message: String) {
        told += "corner: $message"
    }
}

fun telling(): WorkflowDescriptor<Corner, Sync> = WorkflowDescriptor.of(Corner::class.java)
