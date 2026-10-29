package org.itineradev.itinera.request.java.samples

import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.java.JavaRequests
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Sync

/** Notes why each attempt failed. */
class Notes

fun javaHookRequests(): StepPolicyDescriptor<Notes, Sync> {
    val charged = JavaRequests.optionalFromStep("charged", TypeToken.of(Long::class.javaObjectType))
    val limit = JavaRequests.optionalFromWorkflow("limit", TypeToken.of(Long::class.javaObjectType))
    val reason = JavaRequests.optionalReason()
    val error = JavaRequests.optionalError()
    val contributor = Requests.contributor()
    return StepPolicyDescriptor.builder("notes", PolicyFactory(::Notes))
        .onStepRetry(
            listOf(charged, limit, reason, error, contributor),
            SyncHook { _, got ->
                val why = got[reason].map { failed -> failed.code }
                val note =
                    "charged ${got[charged].orElse(0)} of ${got[limit].orElse(0)}: " +
                        why.orElse(got[error].map(Throwable::toString).orElse("no reason"))
                got[contributor].contribute("note", note)
                OnFailure.keepDefault()
            },
        )
        .build()
}
