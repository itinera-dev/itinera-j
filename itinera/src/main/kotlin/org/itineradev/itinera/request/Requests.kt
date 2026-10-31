package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.Handle
import org.itineradev.itinera.engine.InputAsked
import org.itineradev.itinera.journey.Contributor
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
}
