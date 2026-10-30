package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.Name

/**
 * The name of an input adapter, unique among a workflow's adapters.
 *
 * @sample org.itineradev.itinera.samples.names
 */
@ExperimentalItineraApi
public class AdapterName private constructor(name: String) : Name(name, "input adapter") {
    /** Makes input adapter names. */
    public companion object {
        /**
         * The input adapter name [name].
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.samples.names
         */
        @JvmStatic public fun of(name: String): AdapterName = AdapterName(name)
    }
}
