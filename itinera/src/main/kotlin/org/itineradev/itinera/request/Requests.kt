package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.ErrorAsked
import org.itineradev.itinera.engine.FromStepAsked
import org.itineradev.itinera.engine.FromWorkflowAsked
import org.itineradev.itinera.engine.Handle
import org.itineradev.itinera.engine.InputAsked
import org.itineradev.itinera.engine.Known
import org.itineradev.itinera.engine.ReasonAsked
import org.itineradev.itinera.engine.RoleAsked
import org.itineradev.itinera.engine.isRole
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Sync

/**
 * Makes the requests a part of a workflow declares to the builder and reads from its [Got]. Each is
 * a token made once, usually as a constant, whose kinds say which parts may make it.
 * `itinera-kotlin` adds reified forms, such as `Requests.input<Long>("amount")`, and the
 * asynchronous reporters.
 *
 * @sample org.itineradev.itinera.request.samples.requests
 */
@ExperimentalItineraApi
@Suppress("TooManyFunctions")
public object Requests {
    /**
     * A required input: data from the workflow under [key], read as [type]. A missing value aborts
     * the journey with `required data missing`.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    @JvmStatic
    public fun <T> input(key: String, type: TypeToken<T>): Request<BuildKind, T> =
        Asking(InputAsked(key, type.type, required = true), Answer.ITSELF)

    /**
     * An optional input: data from the workflow under [key], read as [type], whose answer is `null`
     * when the data bag holds nothing under the key.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    @JvmStatic
    public fun <T> optionalInput(key: String, type: TypeToken<T>): Request<BuildKind, T?> =
        Asking(InputAsked(key, type.type, required = false), Answer.ITSELF)

    /**
     * A service of [type] under [key], lent for the attempt by the provider the workflow chooses.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    @JvmStatic
    public fun <T : Any> service(key: String, type: Class<T>): ServiceRequest<T> =
        ServiceRequest(key, type)

    /**
     * A contributor, for the data the part contributes to the data bag.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    @JvmStatic
    public fun contributor(): Request<ContributorKind, Contributor> =
        Asking(Handle.CONTRIBUTOR, Answer.ITSELF)

    /**
     * A synchronous step's reporter, for its own events and for setting global metadata.
     *
     * @sample org.itineradev.itinera.request.samples.requests
     */
    @JvmStatic
    public fun stepReporter(): Request<StepReporterKind<Sync>, StepReporter> =
        Asking(Handle.STEP_REPORTER, Answer.ITSELF)

    /**
     * Data the step a hook acts on contributed under [key], committed or not, read as [type]. A
     * missing value aborts the journey with `required data missing`.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun <T> fromStep(key: String, type: TypeToken<T>): Request<FromStepKind, T> =
        Asking(FromStepAsked(key, type.type, required = true), Answer.ITSELF)

    /**
     * Data the step a hook acts on contributed under [key], read as [type], whose answer is `null`
     * when the step contributed nothing under the key.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun <T> optionalFromStep(key: String, type: TypeToken<T>): Request<FromStepKind, T?> =
        Asking(FromStepAsked(key, type.type, required = false), Answer.ITSELF)

    /**
     * Data from the workflow under [key], read as [type] from the data bag, never through an input
     * adapter. A missing value aborts the journey with `required data missing`.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun <T> fromWorkflow(key: String, type: TypeToken<T>): Request<WorkflowDataKind, T> =
        Asking(FromWorkflowAsked(key, type.type, required = true), Answer.ITSELF)

    /**
     * Data from the workflow under [key], read as [type] from the data bag, whose answer is `null`
     * when the data bag holds nothing under the key.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun <T> optionalFromWorkflow(
        key: String,
        type: TypeToken<T>,
    ): Request<WorkflowDataKind, T?> =
        Asking(FromWorkflowAsked(key, type.type, required = false), Answer.ITSELF)

    /**
     * The journey's ID.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun journeyId(): Request<JourneyIdKind, JourneyId> =
        Asking(Known.JOURNEY_ID, Answer.ITSELF)

    /**
     * The name of the step a hook acts on.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun stepName(): Request<StepNameKind, StepName> = Asking(Known.STEP_NAME, Answer.ITSELF)

    /**
     * The attempt a step hook acts on.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun attempt(): Request<AttemptKind, Attempt> = Asking(Known.ATTEMPT, Answer.ITSELF)

    /**
     * The reason the step's attempt reported for failing. When it reported none, the journey aborts
     * with `required data missing`.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun reason(): Request<ReasonKind, Reason> =
        Asking(ReasonAsked(required = true), Answer.ITSELF)

    /**
     * The reason the step's attempt reported for failing, whose answer is `null` when it reported
     * none.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun optionalReason(): Request<ReasonKind, Reason?> =
        Asking(ReasonAsked(required = false), Answer.ITSELF)

    /**
     * The error that ended the step's attempt abnormally. When none did, the journey aborts with
     * `required data missing`.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun error(): Request<ErrorKind, Throwable> =
        Asking(ErrorAsked(required = true), Answer.ITSELF)

    /**
     * The error that ended the step's attempt abnormally, whose answer is `null` when none did.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun optionalError(): Request<ErrorKind, Throwable?> =
        Asking(ErrorAsked(required = false), Answer.ITSELF)

    /**
     * Why the step failed, for `on step failure`.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun failureCause(): Request<FailureCauseKind, FailureCause> =
        Asking(Known.FAILURE_CAUSE, Answer.ITSELF)

    /**
     * Why the step is retried, for `on step retry`.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun retryCause(): Request<RetryCauseKind, RetryCause> =
        Asking(Known.RETRY_CAUSE, Answer.ITSELF)

    /**
     * A synchronous hook's reporter, for its own events and for setting global metadata.
     *
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun hookReporter(): Request<HookReporterKind<Sync>, HookReporter> =
        Asking(Handle.HOOK_REPORTER, Answer.ITSELF)

    /**
     * The role [type], an interface marked `@Role`, which the workflow provides: an object
     * implementing only that interface, which reaches the workflow through it alone.
     *
     * @throws IllegalArgumentException when [type] is not an interface marked `@Role`.
     * @sample org.itineradev.itinera.request.samples.hookRequests
     */
    @JvmStatic
    public fun <R : Any> role(type: Class<R>): Request<RoleKind, R> {
        require(isRole(type)) {
            "A role is an interface marked @Role, which ${type.name} is not."
        }
        return Asking(RoleAsked(type), Answer.ITSELF)
    }
}
