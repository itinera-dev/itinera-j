package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Why a journey was aborted, each written as the specification names it, such as `reporter failed`.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public enum class AbortReason(private val text: String) {
    /** A step's constructor, input adapter or service provider failed. */
    STEP_COULD_NOT_BE_BUILT("step could not be built"),
    /** A step policy failed while it was built for an attempt. */
    POLICY_COULD_NOT_BE_BUILT("policy could not be built"),
    /** A required request has no value. */
    REQUIRED_DATA_MISSING("required data missing"),
    /** A requested value has the wrong type. */
    WRONG_TYPE("wrong type"),
    /** A step hook returned a lifecycle it may not return. */
    INVALID_LIFECYCLE("invalid lifecycle"),
    /** A step, hook or service provider gave something that is not a value. */
    NOT_A_VALUE("not a value"),
    /** A hook, or a role operation it called, failed. */
    HOOK_FAILED("hook failed"),
    /** A reporter, or a dispatcher while dispatching, failed. */
    REPORTER_FAILED("reporter failed"),
    /** A rescue step reported Skipped. */
    @ExperimentalItineraApi RESCUE_SKIPPED("rescue skipped"),
    /** A step, hook or service provider set global metadata under `journey_id`. */
    @ExperimentalItineraApi RESERVED_METADATA_KEY("reserved metadata key"),
    /** Custom code returned something its call cannot return. */
    @ExperimentalItineraApi UNEXPECTED_RESULT("unexpected result");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
