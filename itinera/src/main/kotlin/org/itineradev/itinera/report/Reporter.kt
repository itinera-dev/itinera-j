package org.itineradev.itinera.report

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event

/**
 * Receives the events of a synchronous workflow's journeys and the entries of their global
 * metadata, and decides on its own what to do with each: filter it, map it, forward it, keep it or
 * ignore it. A workflow lists its reporters; each journey has its own, so a reporter never receives
 * another journey's events.
 *
 * A reporter should never fail. One that cannot report an event handles that itself, by logging it,
 * dropping it or keeping it to retry later, and returns normally, so that the reporters after it
 * still receive the event: whatever escapes [report] or [setGlobalMetadata] aborts the journey with
 * `reporter failed`, or refuses it before it starts, except on `journey_aborted`, where it is
 * ignored.
 *
 * @sample org.itineradev.itinera.report.samples.printer
 */
@ExperimentalItineraApi
public fun interface Reporter {
    /**
     * Receives one event of the journey, in sequence order.
     *
     * @sample org.itineradev.itinera.report.samples.printer
     */
    public fun report(event: Event)

    /**
     * Receives one entry of the journey's global metadata: a key, and the value captured when it
     * was set, which a reporter must not change. The journey ID comes first, under `journey_id`,
     * and setting a key again replaces its value, so a reporter keeps the latest value of each key
     * to use with the events that follow. Does nothing unless overridden.
     *
     * @sample org.itineradev.itinera.report.samples.tenants
     */
    public fun setGlobalMetadata(key: String, value: Any?): Unit = Unit
}
