package org.itineradev.itinera.conformance.trace

import java.time.Instant
import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/** Events of a journey of the workflow `orders`, numbered as they are made. */
internal class Events {
    private var sequence = 0L
    private val at = Instant.parse("2026-10-28T02:00:00Z")
    private val journey = JourneyId.of("order-1")
    private val orders = WorkflowName.of("orders")

    private fun next(): Long = ++sequence

    fun started(): Event = JourneyStarted(next(), at, journey, orders, setOf("amount"))

    fun attempt(step: String, number: Int = 1): Event =
        AttemptStarted(next(), at, journey, orders, StepName.of(step), Attempt.of(number))

    fun succeeded(step: String, number: Int = 1): Event =
        StepSucceeded(next(), at, journey, orders, StepName.of(step), Attempt.of(number))

    fun terminated(step: String, message: String): Event =
        StepAbnormalTermination(
            next(),
            at,
            journey,
            orders,
            StepName.of(step),
            Attempt.of(1),
            message,
        )

    fun givenUp(step: String): Event =
        StepGivenUp(
            next(),
            at,
            journey,
            orders,
            StepName.of(step),
            Attempt.of(1),
            GiveUpCause.AbnormalTermination,
        )

    fun aborted(step: String, message: String): Event =
        JourneyAborted(
            next(),
            at,
            journey,
            orders,
            StepCouldNotBeBuilt(StepName.of(step), ByStep, message),
        )

    fun info(step: String, message: String, data: Any?): Event =
        StepInfo(next(), at, journey, orders, StepName.of(step), Attempt.of(1), message, data)
}
