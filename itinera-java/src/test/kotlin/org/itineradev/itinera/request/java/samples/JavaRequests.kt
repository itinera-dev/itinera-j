@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request.java.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.java.JavaRequests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync

fun javaRequests(): StepDescriptor<Sync> {
    val discount = JavaRequests.optionalInput("discount", TypeToken.of(Long::class.javaObjectType))
    val contributor = Requests.contributor()
    return StepDescriptor.builder("price") { got ->
            val taken = got[discount].orElse(0)
            val contributions = got[contributor]
            SyncStepRun {
                contributions.contribute("discount", taken)
                Outcome.success()
            }
        }
        .request(discount)
        .request(contributor)
        .build()
}
