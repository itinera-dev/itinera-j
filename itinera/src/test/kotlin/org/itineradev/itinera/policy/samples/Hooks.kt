@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.policy.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepHook

fun hooks(hook: StepHook, cause: RetryCause, lifecycle: Lifecycle?): String {
    val returned =
        when (lifecycle) {
            null -> "kept the default"
            Lifecycle.FinishWorkflow -> "finished the workflow"
            is Lifecycle.FailWorkflow -> "failed the workflow: ${lifecycle.reason.code}"
        }
    return "$hook after $cause $returned"
}
