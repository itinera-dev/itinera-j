@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.step.Reason

fun decisions(): List<String> {
    val afterSuccess =
        listOf(
            OnSuccess.keepDefault(),
            OnSuccess.finishWorkflow(),
            OnSuccess.failWorkflow(Reason("over the limit", "The order is over the shop's limit")),
            OnSuccess.failWorkflow("over the limit"),
        )
    val afterFailure =
        listOf(
            OnFailure.keepDefault(),
            OnFailure.failWorkflow(Reason("declined", "The card was declined")),
            OnFailure.failWorkflow("declined"),
        )
    return afterSuccess.map(::decided) + afterFailure.map(::decided)
}

private fun decided(decision: OnSuccess): String =
    when {
        decision.keepsDefault -> "keeps the default"
        decision.finishesWorkflow -> "finishes the workflow"
        else -> "fails the workflow: ${decision.failsWorkflow?.code}"
    }

private fun decided(decision: OnFailure): String =
    if (decision.keepsDefault) "keeps the default"
    else "fails the workflow: ${decision.failsWorkflow?.code}"
