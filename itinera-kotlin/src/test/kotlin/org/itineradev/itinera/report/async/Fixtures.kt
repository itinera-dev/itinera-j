package org.itineradev.itinera.report.async

import java.time.Instant
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.workflow.WorkflowName

object Fixtures {
    private val timestamp = Instant.parse("2026-10-27T18:00:00Z")
    private val journey = JourneyId.of("order-42")
    private val workflow = WorkflowName.of("orders")

    val started: Event = JourneyStarted(1L, timestamp, journey, workflow, setOf("amount"))

    val aborted: Event =
        JourneyAborted(2L, timestamp, journey, workflow, ReporterFailed(null, "full"))

    fun received(name: String, event: Event): String = "$name ${event.kind}"

    fun received(name: String, key: String, value: Any?): String = "$name $key=$value"
}

/** Writes what it receives to a log the test shares between reporters. */
class AsyncRecording(private val name: String, private val log: MutableList<String>) :
    AsyncReporter {
    override suspend fun report(event: Event) {
        log += Fixtures.received(name, event)
    }

    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        log += Fixtures.received(name, key, value)
    }
}

/** Throws whatever it receives, as a reporter must not. */
class AsyncFailing(private val thrown: Throwable) : AsyncReporter {
    override suspend fun report(event: Event): Unit = throw thrown

    override suspend fun setGlobalMetadata(key: String, value: Any?): Unit = throw thrown
}
