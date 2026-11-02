package org.itineradev.itinera.report.async

import org.itineradev.itinera.engine.deliverToEveryone
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind

/**
 * The dispatcher an asynchronous executor uses unless it is given another factory: it awaits its
 * reporters one after another, in the order they were added, with every event and every entry of
 * global metadata, so they all receive the same.
 *
 * A reporter that throws stops the delivery: the reporters after it do not receive that event or
 * entry, and the dispatcher throws what it threw. `journey_aborted` alone reaches every reporter,
 * whichever throws, and the dispatcher then throws what the first one threw. An `Error` is never
 * caught.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
 */
public class AsyncDefaultDispatcher : AsyncDispatcher {
    private val reporters = mutableListOf<AsyncReporter>()

    /**
     * Adds a reporter after those already added.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic
    override suspend fun add(reporter: AsyncReporter) {
        reporters += reporter
    }

    /**
     * Delivers one event to each reporter in turn, stopping at the first that throws, unless the
     * event is `journey_aborted`.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic
    override suspend fun dispatch(event: Event) {
        if (event.kind == EventKind.JOURNEY_ABORTED)
            deliverToEveryone(reporters) { reporter -> reporter.report(event) }
        else reporters.forEach { reporter -> reporter.report(event) }
    }

    /**
     * Passes one entry of global metadata to each reporter in turn, stopping at the first that
     * throws.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        reporters.forEach { reporter -> reporter.setGlobalMetadata(key, value) }
    }
}

/**
 * Creates an [AsyncDefaultDispatcher] holding no reporter for each journey.
 *
 * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
 */
public class AsyncDefaultDispatcherFactory : AsyncDispatcherFactory {
    /**
     * A new dispatcher, holding no reporter.
     *
     * @sample org.itineradev.itinera.report.async.samples.asyncDispatching
     */
    @JvmSynthetic override suspend fun create(): AsyncDefaultDispatcher = AsyncDefaultDispatcher()
}
