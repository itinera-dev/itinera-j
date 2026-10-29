package org.itineradev.itinera.event

/**
 * Who made a request whose data could not be resolved: a step for one of its inputs, an input
 * adapter resolving a step's input, a hook, or a service provider supplying a step's service.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface Requester

/**
 * Who requested what a failed attempt left, the failure's reason or the error: a hook of a step
 * policy, for the step it acts on, or a rescue step, for the step it rescues.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface FailureRequester

/**
 * What failed while a step was built: its own constructor, an input adapter with the key of the
 * input it was resolving, or a service provider.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface BuildSource

/**
 * Who gave a value, or set global metadata: a step, a hook or a service provider.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface ValueSource

/**
 * Who gave a reason whose details may not be a value: a step, a rescue step included, or a hook of
 * a step policy returning `FailWorkflow`.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface ReasonSource : ValueSource

/**
 * What returned something its call cannot return: a step's or rescue step's own `run` or
 * constructor, a step hook, an input adapter with the key of the input it was resolving, a service
 * provider with the key of the service, or a step policy's constructor.
 *
 * @sample org.itineradev.itinera.event.samples.aborts
 */
public sealed interface ResultSource

/**
 * What a required request found missing: data under a key, or the failure's reason or the error of
 * a failed attempt.
 *
 * @sample org.itineradev.itinera.event.samples.missing
 */
public sealed interface MissingData {
    /**
     * Data under a key.
     *
     * @sample org.itineradev.itinera.event.samples.missing
     */
    @JvmRecord
    public data class Key(
        /** The key that was requested. */
        val key: String,
        /** Who requested it. */
        val requester: Requester,
    ) : MissingData

    /**
     * The failure's reason, which the attempt did not report.
     *
     * @sample org.itineradev.itinera.event.samples.missing
     */
    @JvmRecord
    public data class FailureReason(
        /** Who requested it. */
        val requester: FailureRequester
    ) : MissingData

    /**
     * The error, which did not end the attempt.
     *
     * @sample org.itineradev.itinera.event.samples.missing
     */
    @JvmRecord
    public data class Error(
        /** Who requested it. */
        val requester: FailureRequester
    ) : MissingData
}

/**
 * What a request found of the wrong type, or could not copy for itself: data under a key, or the
 * failure's reason of a failed attempt, whose details could not be copied.
 *
 * @sample org.itineradev.itinera.event.samples.wrong
 */
public sealed interface WrongData {
    /**
     * Data under a key.
     *
     * @sample org.itineradev.itinera.event.samples.wrong
     */
    @JvmRecord
    public data class Key(
        /** The key that was requested. */
        val key: String,
        /** Who requested it. */
        val requester: Requester,
    ) : WrongData

    /**
     * The failure's reason, whose details could not be copied for the request.
     *
     * @sample org.itineradev.itinera.event.samples.wrong
     */
    @JvmRecord
    public data class FailureReason(
        /** Who requested it. */
        val requester: FailureRequester
    ) : WrongData
}
