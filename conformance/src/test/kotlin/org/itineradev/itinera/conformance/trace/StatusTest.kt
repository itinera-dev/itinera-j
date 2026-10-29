package org.itineradev.itinera.conformance.trace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StatusTest {
    @Test
    fun `a step settled by its own event keeps that status`() {
        val events = Events()
        val stream =
            listOf(
                events.attempt("charge"),
                events.succeeded("charge"),
                events.aborted("ship", "x"),
            )
        assertEquals(Status.SUCCEEDED, statusOf(stream, "charge"))
    }

    @Test
    fun `a step given up failed`() {
        val events = Events()
        assertEquals(
            Status.FAILED,
            statusOf(listOf(events.attempt("charge"), events.givenUp("charge")), "charge"),
        )
    }

    @Test
    fun `a step named by journey_aborted before it settled was aborted`() {
        val events = Events()
        assertEquals(
            Status.ABORTED,
            statusOf(listOf(events.aborted("charge", "no gateway")), "charge"),
        )
    }

    @Test
    fun `a step no attempt of which started was not executed`() {
        val events = Events()
        assertEquals(Status.NOT_EXECUTED, statusOf(listOf(events.started()), "charge"))
    }

    @Test
    fun `a step still being attempted has no status yet`() {
        assertNull(statusOf(listOf(Events().attempt("charge")), "charge"))
    }

    @Test
    fun `a step being rescued is rescuing, and aborted when the journey is aborted then`() {
        val events = Events()
        val rescuing = listOf(events.attempt("charge"), events.rescuing("charge"))
        assertEquals(Status.RESCUING, statusOf(rescuing, "charge"))
        assertEquals(
            Status.ABORTED,
            statusOf(rescuing + events.aborted("charge_rescue", "no client"), "charge"),
        )
    }

    @Test
    fun `a rescued step is rescuing until its next attempt starts, which settles it`() {
        val events = Events()
        val rescued =
            listOf(events.attempt("charge"), events.rescuing("charge"), events.rescued("charge"))
        val again = rescued + events.attempt("charge", 2)
        assertEquals(Status.RESCUING, statusOf(rescued, "charge"))
        assertNull(statusOf(again, "charge"))
        assertEquals(Status.SUCCEEDED, statusOf(again + events.succeeded("charge", 2), "charge"))
    }

    @Test
    fun `attempts are counted from their attempt_started`() {
        val events = Events()
        val stream =
            listOf(events.attempt("charge"), events.attempt("charge", 2), events.attempt("ship"))
        assertEquals(2, attemptsOf(stream, "charge"))
    }
}
