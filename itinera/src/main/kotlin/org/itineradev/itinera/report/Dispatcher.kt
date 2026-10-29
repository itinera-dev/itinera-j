package org.itineradev.itinera.report

import org.itineradev.itinera.event.Event

/**
 * Delivers a synchronous workflow's journey to its reporters: the executor creates one dispatcher
 * for each journey through a [DispatcherFactory], adds the journey's reporters to it, and passes
 * every event and every entry of global metadata through it. When the journey ends, the dispatcher
 * is dropped with its reporters.
 *
 * [DefaultDispatcher] delivers to every reporter, in the order they were added. Another dispatcher
 * decides what adding a reporter means and how it delivers, as a test's dispatcher may hold only
 * its own recording reporter. Whatever it does, the executor guards each reporter, so one that
 * fails receives nothing more, and the others receive only `journey_aborted`.
 *
 * A dispatcher should never fail: whatever escapes it aborts the journey with `reporter failed`, or
 * refuses it before it starts, except on `journey_aborted`, where it is ignored.
 *
 * @sample org.itineradev.itinera.report.samples.dispatching
 */
public interface Dispatcher {
    /**
     * Adds a reporter of the journey.
     *
     * @sample org.itineradev.itinera.report.samples.dispatching
     */
    public fun add(reporter: Reporter)

    /**
     * Delivers one event of the journey.
     *
     * @sample org.itineradev.itinera.report.samples.dispatching
     */
    public fun dispatch(event: Event)

    /**
     * Passes one entry of the journey's global metadata on: a key and its captured value.
     *
     * @sample org.itineradev.itinera.report.samples.dispatching
     */
    public fun setGlobalMetadata(key: String, value: Any?)
}

/**
 * Creates the dispatcher of each journey. An executor calls it once before each journey starts;
 * what it throws, or a dispatcher that fails while the reporters are added, refuses the journey
 * before any event. [DefaultDispatcherFactory] is the default.
 *
 * @sample org.itineradev.itinera.report.samples.dispatching
 */
public fun interface DispatcherFactory {
    /**
     * Creates the dispatcher of one journey.
     *
     * @sample org.itineradev.itinera.report.samples.dispatching
     */
    public fun create(): Dispatcher
}
