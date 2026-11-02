package org.itineradev.itinera.report.testing

import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/** The fixed fields and parts every test event shares. */
object Fixtures {
    const val SEQUENCE = 3L
    val timestamp: Instant = Instant.parse("2026-10-28T02:00:00Z")
    val journey: JourneyId = JourneyId.of("order-42")
    val workflow: WorkflowName = WorkflowName.of("orders")
    val charge: StepName = StepName.of("charge")
    val chargeRescue: StepName = StepName.of("charge_rescue")
    val first: Attempt = Attempt.of(1)
    val second: Attempt = Attempt.of(2)
    val fraudCheck: PolicyName = PolicyName.of("fraud_check")
    val declined = Reason("declined", "The card was declined.")
}

/**
 * Runs `suspend` code that never suspends to its end on the caller's thread, so the tests need no
 * coroutine library.
 */
fun <T> complete(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result -> outcome = result })
    return checkNotNull(outcome) { "The code suspended." }.getOrThrow()
}
