package org.itineradev.itinera.request.samples

import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.samples.Messages
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Sync

/** Watches a step's failed attempts. */
class Watch

fun hookRequests(): StepPolicyDescriptor<Watch, Sync> {
    val charged = Requests.fromStep("charged", TypeToken.of(Long::class.javaObjectType))
    val note = Requests.optionalFromStep("note", TypeToken.of(String::class.java))
    val limit = Requests.fromWorkflow("limit", TypeToken.of(Long::class.javaObjectType))
    val discount =
        Requests.optionalFromWorkflow("discount", TypeToken.of(Long::class.javaObjectType))
    val journey = Requests.journeyId()
    val step = Requests.stepName()
    val attempt = Requests.attempt()
    val maybeReason = Requests.optionalReason()
    val maybeError = Requests.optionalError()
    val cause = Requests.retryCause()
    val reporter = Requests.hookReporter()
    val contributor = Requests.contributor()
    val messages = Requests.role(Messages::class.java)
    val reason = Requests.reason()
    val failure = Requests.failureCause()
    val error = Requests.error()
    return StepPolicyDescriptor.builder("watch", PolicyFactory(::Watch))
        .onStepRetry(
            listOf(
                charged,
                note,
                limit,
                discount,
                journey,
                step,
                attempt,
                maybeReason,
                maybeError,
                cause,
                reporter,
                contributor,
                messages,
            ),
            SyncHook { _, got ->
                val seen =
                    "${got[journey]} ${got[step]} attempt ${got[attempt]} after ${got[cause]}: " +
                        "charged ${got[charged]} of ${got[limit]}, less ${got[discount] ?: 0}, " +
                        "${got[maybeReason]?.code ?: got[maybeError]?.message}, ${got[note]}"
                got[reporter].info("retrying", seen)
                got[contributor].contribute("retried", got[attempt].number)
                got[messages].tell("trying again")
                OnFailure.keepDefault()
            },
        )
        .onStepFailure(
            listOf(reason, failure),
            SyncHook { _, got ->
                OnFailure.failWorkflow("${got[reason].code} after ${got[failure]}")
            },
        )
        .onStepAbnormalTermination(
            listOf(error),
            SyncHook { _, got -> OnFailure.failWorkflow(got[error].javaClass.simpleName) },
        )
        .build()
}
