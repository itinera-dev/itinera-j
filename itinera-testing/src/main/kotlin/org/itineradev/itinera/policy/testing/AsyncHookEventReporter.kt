package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.engine.Lone
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.report.testing.LoneAsyncReporter
import org.itineradev.itinera.report.testing.emittedOf

/**
 * The twin of [HookEventReporter] for asynchronous hooks: it lends an asynchronous hook's reporter
 * outside any journey, and keeps every event emitted through it and the global metadata set. It
 * needs `itinera-kotlin`.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.asyncHookEventReporter
 */
public class AsyncHookEventReporter private constructor(private val lone: Lone) {
    /**
     * A recorder whose reporter keeps what it is given.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.asyncHookEventReporter
     */
    public constructor() : this(Lone(false))

    /**
     * The asynchronous hook reporter it lends.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.asyncHookEventReporter
     */
    public val reporter: AsyncHookReporter = LoneAsyncReporter(lone.hookEmits)

    /**
     * Every event emitted, in order. Each read is a new list.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.asyncHookEventReporter
     */
    public val emitted: List<Emitted>
        get() = lone.events.mapNotNull(::emittedOf)

    /**
     * Each entry of global metadata set, with its latest value, in the order the keys were first
     * set. Each read is a new map.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.asyncHookEventReporter
     */
    public val globalMetadata: Map<String, Any?>
        get() = lone.globalMetadata

    /** Makes recorders. */
    public companion object {
        /**
         * A recorder whose reporter interrupts every call, throwing, and keeps nothing.
         *
         * @sample org.itineradev.itinera.policy.testing.samples.failingAsyncHookEventReporter
         */
        @JvmStatic public fun failing(): AsyncHookEventReporter = AsyncHookEventReporter(Lone(true))
    }
}
