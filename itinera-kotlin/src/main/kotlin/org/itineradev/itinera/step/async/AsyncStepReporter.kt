package org.itineradev.itinera.step.async

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * What an asynchronous step emits its own events through, `step_info`, `step_warning` and
 * `step_error`, and sets global metadata through, made for one attempt and closed when it ends.
 * Each call returns once every reporter has the event, so events keep the order of the calls; a
 * call from a coroutine the step launched outside its own scope, which outlives the attempt, meets
 * a closed reporter.
 *
 * Data given with an event is captured when it is emitted; it must be a value.
 *
 * @sample org.itineradev.itinera.step.async.samples.asyncReporters
 */
public interface AsyncStepReporter {
    /**
     * Emits `step_info` with [message].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @JvmSynthetic public suspend fun info(message: String)

    /**
     * Emits `step_info` with [message] and [data].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @JvmSynthetic public suspend fun info(message: String, data: Any?)

    /**
     * Emits `step_warning` with [message].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @JvmSynthetic public suspend fun warning(message: String)

    /**
     * Emits `step_warning` with [message] and [data].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @JvmSynthetic public suspend fun warning(message: String, data: Any?)

    /**
     * Emits `step_error` with [message].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @JvmSynthetic public suspend fun error(message: String)

    /**
     * Emits `step_error` with [message] and [data].
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @JvmSynthetic public suspend fun error(message: String, data: Any?)

    /**
     * Sets global metadata [key] to [value] on every reporter of the journey. Setting `journey_id`,
     * which only the executor sets, aborts the journey.
     *
     * @throws IllegalStateException when its attempt has ended.
     * @sample org.itineradev.itinera.step.async.samples.asyncReporters
     */
    @ExperimentalItineraApi
    @JvmSynthetic
    public suspend fun setGlobalMetadata(key: String, value: Any?)
}
