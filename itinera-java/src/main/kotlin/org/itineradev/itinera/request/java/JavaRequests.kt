package org.itineradev.itinera.request.java

import java.util.Optional
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.excludingNull
import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken

/**
 * Makes requests in Java's own forms, beside those [Requests] makes.
 *
 * @sample org.itineradev.itinera.request.java.samples.javaRequests
 */
@ExperimentalItineraApi
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
}
