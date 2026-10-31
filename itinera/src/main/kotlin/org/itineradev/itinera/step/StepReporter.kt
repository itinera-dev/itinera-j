package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * What a synchronous step emits its own events through, `step_info`, `step_warning` and
 * `step_error`, and sets global metadata through, made for one attempt and closed when it ends.
 * Each call returns once every reporter has the event, so events keep the order of the calls.
 *
 * Data given with an event is captured when it is emitted; it must be a value.
 *
 * @sample org.itineradev.itinera.step.samples.reporters
 */
@ExperimentalItineraApi
public interface StepReporter {
    /**
     * Emits `step_info` with [message].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun info(message: String)

    /**
     * Emits `step_info` with [message] and [data].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun info(message: String, data: Any?)

    /**
     * Emits `step_warning` with [message].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun warning(message: String)

    /**
     * Emits `step_warning` with [message] and [data].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun warning(message: String, data: Any?)

    /**
     * Emits `step_error` with [message].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun error(message: String)

    /**
     * Emits `step_error` with [message] and [data].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun error(message: String, data: Any?)

    /**
     * Sets global metadata [key] to [value] on every reporter of the journey. Setting `journey_id`,
     * which only the executor sets, aborts the journey.
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.samples.reporters
     */
    public fun setGlobalMetadata(key: String, value: Any?)
}
