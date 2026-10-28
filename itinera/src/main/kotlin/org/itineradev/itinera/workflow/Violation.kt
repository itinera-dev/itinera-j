package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.step.StepName

/**
 * One thing wrong with how a workflow is put together, found before any journey: when it is
 * described or built, or when its instance is handed to an executor. [Violations] lists every one
 * found at once.
 *
 * Those the specification names say so through [named]; the others are mistakes in an annotated
 * class, which the annotations' types cannot catch.
 *
 * @sample org.itineradev.itinera.workflow.samples.violations
 */
@ExperimentalItineraApi
public sealed interface Violation {
    /**
     * The specification's name for this violation, such as `duplicate step name`, if it has one.
     */
    public val named: String?

    /**
     * Two steps of the workflow have this name.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class DuplicateStepName(
        /** The name the steps share. */
        val step: StepName
    ) : Violation {
        override val named: String
            get() = "duplicate step name"
    }

    /**
     * A part of the workflow is in a mode the executor, or the workflow itself, does not accept,
     * such as an asynchronous step in a synchronous workflow.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class ModeNotAccepted(
        /** The part, such as `step charge` or `reporter 2`. */
        val part: String,
        /** The mode accepted. */
        val accepted: Mode,
    ) : Violation {
        override val named: String
            get() = "mode not accepted"
    }

    /**
     * The instance's initial data, or its initial global metadata, holds something that is not a
     * value under this key, or a value whose copy failed, with what the copy threw.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class NotAValue(
        /** Where it is held. */
        val held: Held,
        /** The key it is held under. */
        val key: String,
        /** What the application's code threw while it was copied, if it threw. */
        val error: Throwable?,
    ) : Violation {
        override val named: String?
            get() = null
    }

    /**
     * The instance's initial global metadata sets `journey_id`, which only the executor sets.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    public data object ReservedMetadataKey : Violation {
        override val named: String?
            get() = null
    }

    /**
     * A declaration itinera cannot use, in an annotated class or a hand-written descriptor, such as
     * a step class with no method named `run`, or a hand-written step with a retry budget below 0.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    @JvmRecord
    public data class Misdeclared(
        /**
         * The class, constructor or method, as Java reflection writes it, or a hand-written step,
         * as `step <name>`.
         */
        val declaration: String,
        /** What is wrong with it. */
        val problem: String,
    ) : Violation {
        override val named: String?
            get() = null
    }

    /**
     * Where a workflow instance holds a value it was created with.
     *
     * @sample org.itineradev.itinera.workflow.samples.violations
     */
    public enum class Held {
        /** In its initial data. */
        INITIAL_DATA,
        /** In its initial global metadata. */
        INITIAL_GLOBAL_METADATA,
    }
}
