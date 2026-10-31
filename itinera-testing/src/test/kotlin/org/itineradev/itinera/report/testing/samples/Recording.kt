@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.report.testing.samples

import java.time.Instant
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.DecidedBy
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyFailure
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.JourneySucceeded
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.report.testing.AsyncRecordingReporter
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

private val at = Instant.parse("2026-10-28T02:00:00Z")
private val journey = JourneyId.of("order-42")
private val orders = WorkflowName.of("orders")
private val charge = StepName.of("charge")
private val declined = Reason("declined")

/** What a journey of the workflow `orders` emits when its step `charge` succeeds at once. */
fun succeeding(): List<Event> =
    listOf(
        JourneyStarted(1, at, journey, orders, setOf("amount")),
        StepSucceeded(2, at, journey, orders, charge, Attempt.of(1)),
        JourneySucceeded(3, at, journey, orders, DecidedBy.Default),
    )

fun recording(): List<EventKind> {
    val recorder = RecordingReporter()
    succeeding().forEach(recorder::report)
    return recorder.events.map { it.kind }
}

fun metadata(): Map<String, Any?> {
    val recorder = RecordingReporter()
    recorder.setGlobalMetadata("journey_id", "order-42")
    recorder.setGlobalMetadata("tenant", "acme")
    recorder.setGlobalMetadata("region", "eu")
    recorder.setGlobalMetadata("tenant", "globex")
    return recorder.globalMetadata
}

fun path(): List<String> {
    val recorder = RecordingReporter()
    listOf(
            JourneyStarted(1, at, journey, orders, setOf("amount")),
            StepFailed(2, at, journey, orders, charge, Attempt.of(1), declined),
            StepRetrying(3, at, journey, orders, charge, Attempt.of(1), RetryCause.FAILURE),
            StepFailed(4, at, journey, orders, charge, Attempt.of(2), declined),
            StepGivenUp(
                5,
                at,
                journey,
                orders,
                charge,
                Attempt.of(2),
                GiveUpCause.RetriesExhausted,
            ),
            JourneyFailed(
                6,
                at,
                journey,
                orders,
                charge,
                JourneyFailure.RetriesExhaustedByFailure(declined),
            ),
        )
        .forEach(recorder::report)
    return recorder.path
}

suspend fun asyncRecording(): AsyncRecordingReporter {
    val recorder = AsyncRecordingReporter()
    recorder.setGlobalMetadata("journey_id", "order-42")
    for (event in succeeding()) recorder.report(event)
    return recorder
}
