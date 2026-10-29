package org.itineradev.itinera.engine

import org.itineradev.itinera.engine.Journeys.contributing
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Sync

/** The step `charge` and the rescue steps the tests of rescues give it. */
object Renewals {
    /** The step `charge`, whose first attempt is declined and whose second succeeds. */
    fun declinedOnce(): StepBuilder<Sync> =
        contributing("charge", { Outcome.failure("declined") }, { Outcome.success() })

    /** A rescue step that succeeds. */
    fun renewing(): RescueStepDescriptor<Sync> = rescuing { RescueOutcome.success() }

    /** A rescue step whose run plays [run] with its contributor. */
    fun rescuing(run: (Contributor) -> RescueOutcome): RescueStepDescriptor<Sync> {
        val contributor = Requests.contributor()
        return RescueStepDescriptor.builder { got ->
                val handle = got[contributor]
                SyncRescueRun { run(handle) }
            }
            .request(contributor)
            .build()
    }
}
