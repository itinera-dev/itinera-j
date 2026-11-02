package org.itineradev.itinera.service

/**
 * What a service provider returns: an [Instance] of the service, lent to the step, or [NoInstance],
 * the provider's way of returning nothing, which fails the step's building.
 *
 * @param T the service's type.
 * @sample org.itineradev.itinera.service.samples.providers
 */
public sealed interface Provided<out T : Any> {
    /**
     * An instance of the service, with the [data] the provider supplies with it, which the event
     * saying it was provided carries.
     *
     * @sample org.itineradev.itinera.service.samples.providers
     */
    @JvmRecord
    public data class Instance<out T : Any>(
        /** The instance lent to the step. */
        val instance: T,
        /** The data supplied with it, which must be a value; `null` for none. */
        val data: Any?,
    ) : Provided<T> {
        /** An instance of the service, supplied with no data. */
        public constructor(instance: T) : this(instance, null)
    }

    /**
     * No instance at all, which fails the step's building, as an error the provider throws does.
     *
     * @sample org.itineradev.itinera.service.samples.providers
     */
    public data object NoInstance : Provided<Nothing>

    /** Makes what a provider returns. */
    public companion object {
        /**
         * [NoInstance], typed as what a provider of [T] returns, which Java needs, since it sees
         * [NoInstance] as a raw type.
         *
         * @sample org.itineradev.itinera.service.samples.providers
         */
        @JvmStatic public fun <T : Any> noInstance(): Provided<T> = NoInstance
    }
}
