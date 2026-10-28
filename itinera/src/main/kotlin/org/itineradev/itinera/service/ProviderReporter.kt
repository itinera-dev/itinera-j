package org.itineradev.itinera.service

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * What a synchronous service provider emits its own events through, `journey_info`,
 * `journey_warning` and `journey_error`, which name the provider and the step being built, and sets
 * global metadata through, made for one call of the provider and closed when it returns. Each emit
 * returns once every reporter has the event, so events keep the order of the emits.
 *
 * Data given with an event is captured when it is emitted; it must be a value.
 *
 * @sample org.itineradev.itinera.service.samples.providerReporters
 */
@ExperimentalItineraApi
public interface ProviderReporter {
    /**
     * Emits `journey_info` with [message].
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun info(message: String)

    /**
     * Emits `journey_info` with [message] and [data].
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun info(message: String, data: Any?)

    /**
     * Emits `journey_warning` with [message].
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun warning(message: String)

    /**
     * Emits `journey_warning` with [message] and [data].
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun warning(message: String, data: Any?)

    /**
     * Emits `journey_error` with [message].
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun error(message: String)

    /**
     * Emits `journey_error` with [message] and [data].
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun error(message: String, data: Any?)

    /**
     * Sets global metadata [key] to [value] on every reporter of the journey. Setting `journey_id`,
     * which only the executor sets, aborts the journey.
     *
     * @throws IllegalStateException when its provider has returned.
     * @sample org.itineradev.itinera.service.samples.providerReporters
     */
    public fun setGlobalMetadata(key: String, value: Any?)
}
