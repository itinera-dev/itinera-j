package org.itineradev.itinera.step.testing

import org.itineradev.itinera.engine.Lone
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.report.testing.LoneAsyncReporter
import org.itineradev.itinera.report.testing.emittedOf
import org.itineradev.itinera.step.async.AsyncStepReporter

/**
 * The twin of [StepEventReporter] for asynchronous steps: it lends an asynchronous step's reporter
 * outside any journey, and keeps every event emitted through it and the global metadata set. It
 * needs `itinera-kotlin`.
 *
 * @sample org.itineradev.itinera.step.testing.samples.asyncStepEventReporter
 */
public class AsyncStepEventReporter private constructor(private val lone: Lone) {
    /**
     * A recorder whose reporter keeps what it is given.
     *
     * @sample org.itineradev.itinera.step.testing.samples.asyncStepEventReporter
     */
    public constructor() : this(Lone(false))

    /**
     * The asynchronous step reporter it lends.
     *
     * @sample org.itineradev.itinera.step.testing.samples.asyncStepEventReporter
     */
    public val reporter: AsyncStepReporter = LoneAsyncReporter(lone.stepEmits)

    /**
     * Every event emitted, in order. Each read is a new list.
     *
     * @sample org.itineradev.itinera.step.testing.samples.asyncStepEventReporter
     */
    public val emitted: List<Emitted>
        get() = lone.events.mapNotNull(::emittedOf)

    /**
     * Each entry of global metadata set, with its latest value, in the order the keys were first
     * set. Each read is a new map.
     *
     * @sample org.itineradev.itinera.step.testing.samples.asyncStepEventReporter
     */
    public val globalMetadata: Map<String, Any?>
        get() = lone.globalMetadata

    /** Makes recorders. */
    public companion object {
        /**
         * A recorder whose reporter interrupts every call, throwing, and keeps nothing.
         *
         * @sample org.itineradev.itinera.step.testing.samples.failingAsyncStepEventReporter
         */
        @JvmStatic public fun failing(): AsyncStepEventReporter = AsyncStepEventReporter(Lone(true))
    }
}
