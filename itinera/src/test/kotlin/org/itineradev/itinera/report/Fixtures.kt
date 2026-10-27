package org.itineradev.itinera.report

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.Fixtures.SEQUENCE
import org.itineradev.itinera.event.Fixtures.journey
import org.itineradev.itinera.event.Fixtures.timestamp
import org.itineradev.itinera.event.Fixtures.workflow
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.ReporterFailed

/** Events and reporters for the tests of the core's dispatchers. */
@OptIn(ExperimentalItineraApi::class)
object Fixtures {
    @JvmField
    val started: Event = JourneyStarted(SEQUENCE, timestamp, journey, workflow, setOf("amount"))

    val aborted: Event =
        JourneyAborted(SEQUENCE, timestamp, journey, workflow, ReporterFailed(null, "full"))

    /** What a reporter named [name] received, written to a log shared by every reporter. */
    fun received(name: String, event: Event): String = "$name ${event.kind}"

    fun received(name: String, key: String, value: Any?): String = "$name $key=$value"
}

/** Writes what it receives to a log the test shares between reporters. */
@OptIn(ExperimentalItineraApi::class)
class Recording(private val name: String, private val log: MutableList<String>) : Reporter {
    override fun report(event: Event) {
        log += Fixtures.received(name, event)
    }

    override fun setGlobalMetadata(key: String, value: Any?) {
        log += Fixtures.received(name, key, value)
    }
}

/** Throws whatever it receives, as a reporter must not. */
@OptIn(ExperimentalItineraApi::class)
class Failing(private val thrown: Throwable) : Reporter {
    override fun report(event: Event): Unit = throw thrown

    override fun setGlobalMetadata(key: String, value: Any?): Unit = throw thrown
}
