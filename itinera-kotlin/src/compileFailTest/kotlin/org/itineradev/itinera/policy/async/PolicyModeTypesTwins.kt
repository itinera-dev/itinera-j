package org.itineradev.itinera.policy.async

import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepHookKind
import org.itineradev.itinera.request.async.asyncHookReporter
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Sync

/** What [PolicyModeTypesTest] refuses, written so that it compiles. */
internal object PolicyModeTypesTwins {
    class Audit

    val audit = StepPolicyDescriptor.builder("audit", PolicyFactory(::Audit))
    val asyncAudit = asyncStepPolicy("audit", PolicyFactory(::Audit))

    val hooked = audit.onStepSuccess(SyncHook { _, _ -> OnSuccess.keepDefault() })
    val asyncHooked = asyncAudit.onStepSuccess(AsyncHook { _, _ -> OnSuccess.keepDefault() })

    fun read(got: Got<StepHookKind<Sync>>) = got[Requests.hookReporter()]

    fun readAsync(got: Got<StepHookKind<Async>>) = got[Requests.asyncHookReporter()]
}
