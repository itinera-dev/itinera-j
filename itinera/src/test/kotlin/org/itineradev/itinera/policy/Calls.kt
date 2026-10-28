@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Answers
import org.itineradev.itinera.workflow.Sync

/** Calls the synchronous step hook [definition] on [policy], answering its requests in order. */
fun <P, K, R> called(
    definition: HookDefinition<Sync, P, K, R>?,
    policy: P,
    vararg answers: Any?,
): R {
    val defined = checkNotNull(definition)
    return (defined.call as SyncHook<P, K, R>).call(policy, answered(defined, answers))
}

/**
 * Calls the synchronous workflow hook [definition] on [policy], answering its requests in order.
 */
fun <P, K> calledOnWorkflow(
    definition: HookDefinition<Sync, P, K, Unit>?,
    policy: P,
    vararg answers: Any?,
) {
    val defined = checkNotNull(definition)
    (defined.call as SyncWorkflowHook<P, K>).call(policy, answered(defined, answers))
}

private fun <K> answered(
    definition: HookDefinition<*, *, K, *>,
    answers: Array<out Any?>,
): Answers<K> = Answers(definition.requests.zip(answers).toMap())
