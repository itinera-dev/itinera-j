package org.itineradev.itinera.request.java

import java.util.Optional
import org.itineradev.itinera.engine.excludingNull
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.ErrorKind
import org.itineradev.itinera.request.FromStepKind
import org.itineradev.itinera.request.ReasonKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.WorkflowDataKind
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.value.TypeToken

/**
 * Makes requests in Java's own forms, beside those [Requests] makes.
 *
 * @sample org.itineradev.itinera.request.java.samples.javaRequests
 */
public object JavaRequests {
    /**
     * An optional input: data from the workflow under [key], read as [type], whose answer is an
     * empty `Optional` when the data bag holds nothing under the key. Since an `Optional` cannot
     * hold `null`, a stored `null` aborts the journey with `wrong type`.
     *
     * @sample org.itineradev.itinera.request.java.samples.javaRequests
     */
    @JvmStatic
    public fun <T : Any> optionalInput(
        key: String,
        type: TypeToken<T>,
    ): Request<BuildKind, Optional<T>> =
        excludingNull(Requests.optionalInput(key, type), presentOrEmpty)

    /**
     * Data the step a hook acts on contributed under [key], read as [type], whose answer is an
     * empty `Optional` when the step contributed nothing under the key, or contributed `null`.
     *
     * @sample org.itineradev.itinera.request.java.samples.javaHookRequests
     */
    @JvmStatic
    public fun <T : Any> optionalFromStep(
        key: String,
        type: TypeToken<T>,
    ): Request<FromStepKind, Optional<T>> =
        excludingNull(Requests.optionalFromStep(key, type), presentOrEmpty)

    /**
     * Data from the workflow under [key], read as [type] from the data bag, whose answer is an
     * empty `Optional` when the data bag holds nothing under the key, or holds `null`.
     *
     * @sample org.itineradev.itinera.request.java.samples.javaHookRequests
     */
    @JvmStatic
    public fun <T : Any> optionalFromWorkflow(
        key: String,
        type: TypeToken<T>,
    ): Request<WorkflowDataKind, Optional<T>> =
        excludingNull(Requests.optionalFromWorkflow(key, type), presentOrEmpty)

    /**
     * The reason the step's attempt reported for failing, whose answer is an empty `Optional` when
     * it reported none.
     *
     * @sample org.itineradev.itinera.request.java.samples.javaHookRequests
     */
    @JvmStatic
    public fun optionalReason(): Request<ReasonKind, Optional<Reason>> =
        excludingNull(Requests.optionalReason(), presentOrEmpty)

    /**
     * The error that ended the step's attempt abnormally, whose answer is an empty `Optional` when
     * none did.
     *
     * @sample org.itineradev.itinera.request.java.samples.javaHookRequests
     */
    @JvmStatic
    public fun optionalError(): Request<ErrorKind, Optional<Throwable>> =
        excludingNull(Requests.optionalError(), presentOrEmpty)
}
