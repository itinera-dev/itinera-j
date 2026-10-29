package org.itineradev.itinera.policy.samples

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** Notes what each charge was, and fails the journey when a charge is declined. */
class Audit {
    fun charged(amount: Long, contributor: Contributor): OnSuccess {
        contributor.contribute("audit", "charged $amount")
        return OnSuccess.keepDefault()
    }

    fun failed(reason: Reason): OnFailure =
        if (reason.code == "declined") OnFailure.failWorkflow(reason) else OnFailure.keepDefault()
}

/** Notes in the data bag that the journey succeeded. */
class Ledger {
    fun succeeded(journey: JourneyId, contributor: Contributor) {
        contributor.contribute("ledger", "paid $journey")
    }
}

/** A shop. */
class Shop

fun audit(): StepPolicyDescriptor<Audit, Sync> {
    val charged = Requests.fromStep("charged", TypeToken.of(Long::class.javaObjectType))
    val contributor = Requests.contributor()
    val reason = Requests.reason()
    return StepPolicyDescriptor.builder("audit", PolicyFactory(::Audit))
        .onStepSuccess(
            listOf(charged, contributor),
            SyncHook { policy, got -> policy.charged(got[charged], got[contributor]) },
        )
        .onStepFailure(listOf(reason), SyncHook { policy, got -> policy.failed(got[reason]) })
        .build()
}

fun ledger(): WorkflowPolicyDescriptor<Ledger, Sync> {
    val journey = Requests.journeyId()
    val contributor = Requests.contributor()
    return WorkflowPolicyDescriptor.builder("ledger", PolicyFactory(::Ledger))
        .onWorkflowSuccess(
            listOf(journey, contributor),
            SyncWorkflowHook { policy, got -> policy.succeeded(got[journey], got[contributor]) },
        )
        .build()
}

fun policies(): WorkflowDescriptor<Shop, Sync> {
    val charge =
        StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }.policy(audit())
    return WorkflowDescriptor.builder(Shop::class.java, "shop")
        .step(charge.build())
        .policy(ledger())
        .build()
}
