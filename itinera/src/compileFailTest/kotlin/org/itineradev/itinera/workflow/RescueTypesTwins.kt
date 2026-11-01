package org.itineradev.itinera.workflow

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken

/** What [RescueTypesTest] refuses, written so that it compiles. */
internal object RescueTypesTwins {
    val rescue = RescueStepDescriptor.builder { SyncRescueRun { RescueOutcome.success() } }

    fun read(got: Got<RescueKind<Sync>>) =
        listOf(
            got[Requests.reason()],
            got[Requests.error()],
            got[Requests.fromStep("token", TypeToken.of(String::class.java))],
            got[Requests.contributor()],
            got[Requests.stepReporter()],
        )

    val rescued =
        StepDescriptor.builder("charge") { SyncStepRun { Outcome.success() } }
            .rescue(rescue.build())
}
