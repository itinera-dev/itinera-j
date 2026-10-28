package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/** What [EventTypesTest] refuses, written so that it compiles. */
internal object EventTypesTwins {
    val at: Instant = Instant.EPOCH
    val id = JourneyId.of("order-1")
    val orders = WorkflowName.of("orders")
    val pack = StepName.of("pack")
    val first = Attempt.of(1)

    val contribution = ContributionCommitted(1, at, id, orders, "k", StepSource(pack, first))
    val info =
        JourneyInfo(
            1,
            at,
            id,
            orders,
            StepHookSource(PolicyName.of("audit"), StepHook.ON_STEP_FAILURE, pack, first),
            "m",
            null,
        )
    val retrying = StepRetrying(1, at, id, orders, pack, first, RetryCause.FAILURE)
    val rescuing = StepRescuing(1, at, id, orders, pack, first, RescueCause.FAILURE, pack)
    val rescued = StepRescued(1, at, id, orders, pack, first, pack)
}
