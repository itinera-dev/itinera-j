package org.itineradev.itinera.policy.async.samples

import kotlinx.coroutines.yield
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.async.AsyncHook
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.policy.async.AsyncWorkflowHook
import org.itineradev.itinera.policy.async.asyncStepPolicy
import org.itineradev.itinera.policy.async.asyncWorkflowPolicy
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.async.asyncHookReporter
import org.itineradev.itinera.request.kotlin.fromStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.async.AsyncStepRun
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.async.asyncBuilder
import org.itineradev.itinera.workflow.async.asyncStep

/** Reports each charge, and fails the journey when a charge is declined. */
class Audit {
    suspend fun charged(amount: Long, reporter: AsyncHookReporter): OnSuccess {
        reporter.info("charged", amount)
        return OnSuccess.keepDefault()
    }

    suspend fun failed(reason: Reason): OnFailure {
        yield()
        return if (reason.code == "declined") OnFailure.failWorkflow(reason)
        else OnFailure.keepDefault()
    }
}

/** Notes in the data bag that the journey succeeded. */
class Ledger {
    suspend fun succeeded(journey: JourneyId, contributor: Contributor) {
        yield()
        contributor.contribute("ledger", "paid $journey")
    }
}

/** A shop. */
class Shop

fun asyncAudit(): StepPolicyDescriptor<Audit, Async> {
    val charged = Requests.fromStep<Long>("charged")
    val reporter = Requests.asyncHookReporter()
    val reason = Requests.reason()
    return asyncStepPolicy("audit", PolicyFactory(::Audit))
        .onStepSuccess(
            listOf(charged, reporter),
            AsyncHook { policy, got -> policy.charged(got[charged], got[reporter]) },
        )
        .onStepFailure(listOf(reason), AsyncHook { policy, got -> policy.failed(got[reason]) })
        .build()
}

fun asyncLedger(): WorkflowPolicyDescriptor<Ledger, Async> {
    val journey = Requests.journeyId()
    val contributor = Requests.contributor()
    return asyncWorkflowPolicy("ledger", PolicyFactory(::Ledger))
        .onWorkflowSuccess(
            listOf(journey, contributor),
            AsyncWorkflowHook { policy, got -> policy.succeeded(got[journey], got[contributor]) },
        )
        .build()
}

fun asyncPolicies(): WorkflowDescriptor<Shop, Async> =
    asyncBuilder<Shop>("shop")
        .step(
            asyncStep("charge") { AsyncStepRun { Outcome.success() } }.policy(asyncAudit()).build()
        )
        .policy(asyncLedger())
        .build()
