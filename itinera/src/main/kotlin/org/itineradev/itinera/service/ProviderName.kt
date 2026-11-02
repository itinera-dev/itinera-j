package org.itineradev.itinera.service

import org.itineradev.itinera.Name

/**
 * The name of a service provider, which every event from or about it carries.
 *
 * @sample org.itineradev.itinera.samples.names
 */
public class ProviderName private constructor(name: String) : Name(name, "service provider") {
    /** Makes service provider names. */
    public companion object {
        /**
         * The service provider name [name].
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.samples.names
         */
        @JvmStatic public fun of(name: String): ProviderName = ProviderName(name)
    }
}
