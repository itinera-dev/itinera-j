package org.itineradev.itinera.step.testing

import org.itineradev.itinera.engine.Lone
import org.itineradev.itinera.report.testing.Emitted
import org.itineradev.itinera.report.testing.emittedOf
import org.itineradev.itinera.step.StepReporter

/**
 * Lends a step's reporter outside any journey, so that a test calls a synchronous step's run
 * itself, and keeps every event emitted through it and the global metadata set. The reporter
 * behaves as in a journey: one made [failing] interrupts every call, throwing, and keeps nothing,
 * and setting the reserved entry `journey_id` interrupts that call and every later one.
 * [AsyncStepEventReporter] is its twin for asynchronous steps.
 *
 * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
 */
public class StepEventReporter private constructor(private val lone: Lone) {
    /**
     * A recorder whose reporter keeps what it is given.
     *
     * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
     */
    public constructor() : this(Lone(false))

    /**
     * The step reporter it lends.
     *
     * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
     */
    public val reporter: StepReporter
        get() = lone.stepReporter

    /**
     * Every event emitted, in order. Each read is a new list.
     *
     * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
     */
    public val emitted: List<Emitted>
        get() = lone.events.mapNotNull(::emittedOf)

    /**
     * Each entry of global metadata set, with its latest value, in the order the keys were first
     * set. Each read is a new map.
     *
     * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
     */
    public val globalMetadata: Map<String, Any?>
        get() = lone.globalMetadata

    /** Makes recorders. */
    public companion object {
        /**
         * A recorder whose reporter interrupts every call, throwing, and keeps nothing.
         *
         * @sample org.itineradev.itinera.step.testing.samples.failingStepEventReporter
         */
        @JvmStatic public fun failing(): StepEventReporter = StepEventReporter(Lone(true))
    }
}
