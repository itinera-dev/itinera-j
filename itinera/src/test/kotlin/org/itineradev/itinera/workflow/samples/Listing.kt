@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.workflow.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.samples.Quiet
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.samples.PaymentGateway
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.ListedStep
import org.itineradev.itinera.workflow.Position
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The listing of a workflow whose charge is audited and counted, priced by an adapter, paid through
 * a gateway and rescued by a renewal of the card, before it ships: the listing's first entry is the
 * charge, at position 1.
 */
fun listing(): List<ListedStep> {
    val payments = Requests.service("payments", PaymentGateway::class.java)
    val audit =
        StepPolicyDescriptor.builder("audit", PolicyFactory(::Quiet))
            .onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
            .build()
    val count =
        StepPolicyDescriptor.builder("count", PolicyFactory(::Quiet))
            .onStepFailure(SyncHook { _, _ -> OnFailure.keepDefault() })
            .build()
    val renewal =
        RescueStepDescriptor.builder { SyncRescueRun { RescueOutcome.success() } }
            .request(payments)
            .build()
    val charge =
        StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
            .request(payments)
            .policy(audit)
            .policy(count)
            .rescue(renewal)
            .build()
    val gateway =
        ProviderDescriptor.builder<Orders, PaymentGateway>(
                "gateway",
                "payments",
                PaymentGateway::class.java,
            ) { _, _ ->
                Provided.Instance(PaymentGateway { amount -> "paid $amount" })
            }
            .build()
    val listing =
        WorkflowDescriptor.builder(Orders::class.java, "orders")
            .step(charge)
            .step(StepDescriptor.builder("ship") { SyncStepRun { Outcome.success() } }.build())
            .inputAdapter(
                InputAdapterDescriptor.builder<Orders>("pricing", "charge") { _, _ ->
                        Supplied.FromDataBag
                    }
                    .build()
            )
            .provider(gateway)
            .build()
            .listing()
    check(listing.first().position == Position.of(1))
    return listing
}
