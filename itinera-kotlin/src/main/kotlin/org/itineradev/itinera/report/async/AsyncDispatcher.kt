package org.itineradev.itinera.report.async

import org.itineradev.itinera.event.Event

/**
 * Delivers an asynchronous workflow's journey to its reporters: the executor creates one dispatcher
 * for each journey through an [AsyncDispatcherFactory], adds the journey's reporters to it, and
 * passes every event and every entry of global metadata through it, awaiting each. When the journey
 * ends, the dispatcher is dropped with its reporters.
 *
 * [AsyncDefaultDispatcher] delivers to every reporter, in the order they were added. Another
 * dispatcher decides what adding a reporter means and how it delivers. Whatever it does, the
 * executor guards each reporter, so one that fails receives nothing more, and the others receive
 * only `journey_aborted`.
 *
 * A dispatcher should never fail: whatever escapes it aborts the journey with `reporter failed`, or
 * refuses it before it starts, except on `journey_aborted`, where it is ignored.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
 */
public interface AsyncDispatcher {
    /**
     * Adds a reporter of the journey.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic public suspend fun add(reporter: AsyncReporter)

    /**
     * Delivers one event of the journey.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic public suspend fun dispatch(event: Event)

    /**
     * Passes one entry of the journey's global metadata on: a key and its captured value.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic public suspend fun setGlobalMetadata(key: String, value: Any?)
}

/**
 * Creates the dispatcher of each journey of an asynchronous workflow. An executor calls it once
 * before each journey starts; what it throws, or a dispatcher that fails while the reporters are
 * added, refuses the journey before any event. [AsyncDefaultDispatcherFactory] is the default.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
 */
public fun interface AsyncDispatcherFactory {
    /**
     * Creates the dispatcher of one journey.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic public suspend fun create(): AsyncDispatcher
}
