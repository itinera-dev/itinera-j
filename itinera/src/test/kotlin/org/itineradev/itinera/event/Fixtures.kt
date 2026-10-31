package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.WorkflowName

/** The fixed fields and parts every test event shares. */
object Fixtures {
    const val SEQUENCE = 7L
    val timestamp: Instant = Instant.parse("2026-10-27T18:00:00Z")
    val journey: JourneyId = JourneyId.of("order-42")
    val workflow: WorkflowName = WorkflowName.of("orders")
    val charge: StepName = StepName.of("charge")
    val first: Attempt = Attempt.of(1)
    val audit: PolicyName = PolicyName.of("audit")
    val pricing: AdapterName = AdapterName.of("pricing")
    val payments: ProviderName = ProviderName.of("payments")
    val declined = Reason("declined")
    val retryHook = DecidingHook(audit, StepHook.ON_STEP_RETRY)
    val closing = ByWorkflowHook(audit, WorkflowHook.ON_WORKFLOW_SUCCESS)
    val stepSource = StepSource(charge, first)
    val stepHookSource = StepHookSource(audit, StepHook.ON_STEP_RETRY, charge, first)
}
