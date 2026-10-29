package org.itineradev.itinera.event

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * The kind of an event, one per event of the specification's catalogue and per event a step, hook
 * or service provider emits. Each is written as the specification names it, in snake_case, such as
 * `step_failed`, and [parse] reads it back.
 *
 * @sample org.itineradev.itinera.event.samples.kinds
 */
public enum class EventKind {
    /** The journey started. */
    JOURNEY_STARTED,
    /** An attempt of a step started. */
    ATTEMPT_STARTED,
    /** An input adapter supplied a value for a step's input. */
    INPUT_ADAPTER_SUPPLIED,
    /** An input adapter failed for a step's input. */
    INPUT_ADAPTER_FAILED,
    /** An optional request had no value. */
    OPTIONAL_INPUT_ABSENT,
    /** A service provider made an instance of a step's service. */
    @ExperimentalItineraApi SERVICE_PROVIDED,
    /** A service provider failed for a step's service. */
    @ExperimentalItineraApi SERVICE_PROVIDER_FAILED,
    /** A step, hook or service provider set an entry of global metadata. */
    @ExperimentalItineraApi GLOBAL_METADATA_SET,
    /** An attempt reported Success. */
    STEP_SUCCEEDED,
    /** An attempt reported Failure. */
    STEP_FAILED,
    /** An attempt reported Skipped. */
    STEP_SKIPPED,
    /** An error escaped a running step. */
    STEP_ABNORMAL_TERMINATION,
    /** A hook returned without failing. */
    HOOK_CALLED,
    /** A contribution reached the data bag. */
    CONTRIBUTION_COMMITTED,
    /** A skipped step's contributions were discarded. */
    CONTRIBUTIONS_DISCARDED,
    /** A committed key replaced an earlier value. */
    DATA_OVERWRITTEN,
    /** The journey was aborted. */
    JOURNEY_ABORTED,
    /** The executor decided to attempt a step again. */
    STEP_RETRYING,
    /** The executor decided to rescue a step. */
    @ExperimentalItineraApi STEP_RESCUING,
    /** A step's rescue step succeeded, and the step will be attempted again. */
    @ExperimentalItineraApi STEP_RESCUED,
    /** The executor decided not to attempt a step again. */
    STEP_GIVEN_UP,
    /** The executor decided that the journey succeeds. */
    JOURNEY_SUCCEEDED,
    /** The executor decided that the journey fails. */
    JOURNEY_FAILED,
    /** A step reported information. */
    STEP_INFO,
    /** A step reported a warning. */
    STEP_WARNING,
    /** A step reported an error. */
    STEP_ERROR,
    /** A hook or service provider reported information. */
    JOURNEY_INFO,
    /** A hook or service provider reported a warning. */
    JOURNEY_WARNING,
    /** A hook or service provider reported an error. */
    JOURNEY_ERROR;

    /** The name, as the specification writes it. */
    override fun toString(): String = name.lowercase()

    /** Reads kinds back from their names. */
    public companion object {
        private val byName: Map<String, EventKind> = entries.associateBy(EventKind::toString)

        /**
         * The kind named [name] in snake_case, as the specification writes it, or `null` when no
         * kind has that name.
         *
         * @sample org.itineradev.itinera.event.samples.kinds
         */
        @JvmStatic public fun parse(name: String): EventKind? = byName[name]
    }
}
