package org.itineradev.itinera.event

import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.step.StepName

/**
 * Why a journey was aborted, as `journey_aborted` tells it: the abort reason, the step during which
 * it happened when there was one, and exactly the details that reason carries, among them the
 * message of the error when failing custom code caused the abort. A class for each reason, and for
 * each form of `not a value`.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface JourneyAbort {
    /** Why the journey was aborted. */
    public val reason: AbortReason

    /** The name of the step during which it was aborted, if any. */
    public val step: StepName?

    /** The message of the error, when failing custom code caused the abort. */
    public val errorMessage: String?
}

/**
 * A step's constructor, an input adapter resolving one of its inputs, or a service provider for one
 * of its services, failed; or the adapter supplied what is not a value, or the provider returned no
 * instance.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class StepCouldNotBeBuilt(
    override val step: StepName,
    /** What failed. */
    val source: BuildSource,
    override val errorMessage: String?,
) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.STEP_COULD_NOT_BE_BUILT
}

/**
 * A step policy failed while it was built for an attempt.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class PolicyCouldNotBeBuilt(
    override val step: StepName,
    /** The policy's name. */
    val policy: PolicyName,
    override val errorMessage: String,
) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.POLICY_COULD_NOT_BE_BUILT
}

/**
 * A required request has no value.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class RequiredDataMissing(
    override val step: StepName?,
    /** What was requested, and by whom. */
    val missing: MissingData,
) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.REQUIRED_DATA_MISSING

    override val errorMessage: String?
        get() = null
}

/**
 * A requested value has the wrong type, or could not be copied for the request.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
@JvmRecord
public data class WrongType(
    override val step: StepName?,
    /** What was requested, and by whom. */
    val wrong: WrongData,
    /**
     * The message of what the application's code threw while the value was copied for the request,
     * if it threw; `null` when the value is of another type.
     */
    override val errorMessage: String?,
) : JourneyAbort {
    override val reason: AbortReason
        get() = AbortReason.WRONG_TYPE
}
