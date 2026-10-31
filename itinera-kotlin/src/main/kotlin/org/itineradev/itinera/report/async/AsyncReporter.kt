package org.itineradev.itinera.report.async

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.workflow.Async

/**
 * Receives the events of an asynchronous workflow's journeys and the entries of their global
 * metadata, as `suspend` functions the journey awaits, and decides on its own what to do with each:
 * filter it, map it, forward it, keep it or ignore it. A workflow lists its reporters; each journey
 * has its own, so a reporter never receives another journey's events.
 *
 * A reporter should never fail. One that cannot report an event handles that itself and returns
 * normally, so that the reporters after it still receive the event: whatever escapes [report] or
 * [setGlobalMetadata] aborts the journey with `reporter failed`, or refuses it before it starts,
 * except on `journey_aborted`, where it is ignored.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncPrinter
 */
@ExperimentalItineraApi
public fun interface AsyncReporter : ReporterOf<Async> {
    /**
     * Receives one event of the journey, in sequence order; the journey goes on once it returns.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncPrinter
     */
    @JvmSynthetic public suspend fun report(event: Event)

    /**
     * Receives one entry of the journey's global metadata: a key, and the value captured when it
     * was set, which a reporter must not change. The journey ID comes first, under `journey_id`,
     * and setting a key again replaces its value, so a reporter keeps the latest value of each key
     * to use with the events that follow. Does nothing unless overridden.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncTenants
     */
    @JvmSynthetic public suspend fun setGlobalMetadata(key: String, value: Any?): Unit = Unit
}
