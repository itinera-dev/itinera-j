package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.step.StepName

/**
 * A step, a hook or a service provider contributed, emitted, set as global metadata, gave in a
 * reason's details, or supplied with an instance, something that is not a value. It names the key
 * of a contribution or an entry of global metadata, or the kind of the event whose data it was, and
 * never both. When copying it threw, it carries the message of what was thrown.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@ExperimentalItineraApi
public sealed interface NotAValue : JourneyAbort {
    /** Who gave it. */
    public val source: ValueSource

    /**
     * A contribution, or an entry of global metadata, that is not a value.
     *
     * @sample org.itineradev.itinera.event.samples.aborts
     */
    @JvmRecord
    public data class UnderKey(
        override val step: StepName?,
        override val source: ValueSource,
        /** The key it was given under. */
        val key: String,
        /** The message of what the application's code threw while it was copied, if it threw. */
        override val errorMessage: String?,
    ) : NotAValue {
        override val reason: AbortReason
            get() = AbortReason.NOT_A_VALUE
    }

    /**
     * Data given with an event a step, a hook or a service provider emitted, or that a provider
     * supplied with an instance, for `service_provided`, that is not a value.
     *
     * @sample org.itineradev.itinera.event.samples.aborts
     */
    @JvmRecord
    public data class InEvent(
        override val step: StepName?,
        override val source: ValueSource,
        /** The kind of the event it was given with. */
        val eventKind: EventKind,
        /** The message of what the application's code threw while it was copied, if it threw. */
        override val errorMessage: String?,
    ) : NotAValue {
        override val reason: AbortReason
            get() = AbortReason.NOT_A_VALUE
    }

    /**
     * A reason's details that are not a value.
     *
     * @sample org.itineradev.itinera.event.samples.aborts
     */
    @JvmRecord
    public data class InDetails(
        override val step: StepName?,
        override val source: ReasonSource,
        /** The message of what the application's code threw while it was copied, if it threw. */
        override val errorMessage: String?,
    ) : NotAValue {
        override val reason: AbortReason
            get() = AbortReason.NOT_A_VALUE
    }
}
