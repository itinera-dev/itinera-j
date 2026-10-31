package org.itineradev.itinera.policy

/**
 * What a synchronous hook emits its own events through, `journey_info`, `journey_warning` and
 * `journey_error`, which name its policy and hook, and sets global metadata through, made for one
 * call of the hook and closed when it returns. Each emit returns once every reporter has the event,
 * so events keep the order of the emits.
 *
 * Data given with an event is captured when it is emitted; it must be a value.
 *
 * @sample org.itineradev.itinera.policy.samples.hookReporters
 */
public interface HookReporter {
    /**
     * Emits `journey_info` with [message].
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun info(message: String)

    /**
     * Emits `journey_info` with [message] and [data].
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun info(message: String, data: Any?)

    /**
     * Emits `journey_warning` with [message].
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun warning(message: String)

    /**
     * Emits `journey_warning` with [message] and [data].
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun warning(message: String, data: Any?)

    /**
     * Emits `journey_error` with [message].
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun error(message: String)

    /**
     * Emits `journey_error` with [message] and [data].
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun error(message: String, data: Any?)

    /**
     * Sets global metadata [key] to [value] on every reporter of the journey. Setting `journey_id`,
     * which only the executor sets, aborts the journey.
     *
     * @throws IllegalStateException when its hook has returned.
     * @sample org.itineradev.itinera.policy.samples.hookReporters
     */
    public fun setGlobalMetadata(key: String, value: Any?)
}
