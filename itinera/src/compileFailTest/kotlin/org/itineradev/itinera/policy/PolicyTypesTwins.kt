package org.itineradev.itinera.policy

import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** What [PolicyTypesTest] refuses, written so that it compiles. */
internal object PolicyTypesTwins {
    class Audit

    @Role
    fun interface Messages {
        fun tell(message: String)
    }

    class Shop

    class Corner : Messages {
        override fun tell(message: String) = Unit
    }

    val audit = StepPolicyDescriptor.builder("audit", PolicyFactory(::Audit))
    val notify = WorkflowPolicyDescriptor.builder("notify", PolicyFactory(::Audit))

    val failing = audit.onStepFailure(SyncHook { _, _ -> OnFailure.keepDefault() })
    val hooked = audit.onStepFailure { _, _ -> OnFailure.keepDefault() }
    val succeeding = audit.onStepSuccess { _, _ -> OnSuccess.keepDefault() }
    val builtStepPolicy = StepPolicyDescriptor.builder("audit") { Audit() }
    val builtWorkflowPolicy = WorkflowPolicyDescriptor.builder("audit") { Audit() }
    val succeedingWithRequests =
        audit.onStepSuccess(
            listOf(Requests.journeyId()),
            SyncHook { _, _ -> OnSuccess.keepDefault() },
        )
    val failingWithRequests =
        audit.onStepFailure(
            listOf(Requests.journeyId()),
            SyncHook { _, _ -> OnFailure.keepDefault() },
        )
    val workflowSucceeding =
        notify.onWorkflowSuccess(listOf(Requests.journeyId()), SyncWorkflowHook { _, _ -> })
    val workflowFailing =
        notify.onWorkflowFailure(listOf(Requests.journeyId()), SyncWorkflowHook { _, _ -> })
    val corner = WorkflowDescriptor.builder(Corner::class.java, "corner").role(Messages::class.java)

    fun attach(
        workflow: WorkflowBuilder<Shop, Sync>,
        policy: WorkflowPolicyDescriptor<Audit, Sync>,
    ) = workflow.policy(policy)

    fun attach(step: StepBuilder<Sync>, policy: StepPolicyDescriptor<Audit, Sync>) =
        step.policy(policy)
}
