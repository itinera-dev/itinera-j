package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.engine.Lone
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.report.testing.emittedOf

/**
 * Lends a hook's reporter outside any journey, so that a test calls a synchronous hook itself, and
 * keeps every event emitted through it and the global metadata set. The reporter behaves as in a
 * journey: one made [failing] interrupts every call, throwing, and keeps nothing, and setting the
 * reserved entry `journey_id` interrupts that call and every later one. [AsyncHookEventReporter] is
 * its twin for asynchronous hooks.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.hookEventReporter
 */
public class HookEventReporter private constructor(private val lone: Lone) {
    /**
     * A recorder whose reporter keeps what it is given.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.hookEventReporter
     */
    public constructor() : this(Lone(false))

    /**
     * The hook reporter it lends.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.hookEventReporter
     */
    public val reporter: HookReporter
        get() = lone.hookReporter

    /**
     * Every event emitted, in order. Each read is a new list.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.hookEventReporter
     */
    public val emitted: List<Emitted>
        get() = lone.events.mapNotNull(::emittedOf)

    /**
     * Each entry of global metadata set, with its latest value, in the order the keys were first
     * set. Each read is a new map.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.hookEventReporter
     */
    public val globalMetadata: Map<String, Any?>
        get() = lone.globalMetadata

    /** Makes recorders. */
    public companion object {
        /**
         * A recorder whose reporter interrupts every call, throwing, and keeps nothing.
         *
         * @sample org.itineradev.itinera.policy.testing.samples.failingHookEventReporter
         */
        @JvmStatic public fun failing(): HookEventReporter = HookEventReporter(Lone(true))
    }
}
