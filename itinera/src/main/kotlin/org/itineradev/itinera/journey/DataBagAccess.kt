package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.value.TypeToken

/**
 * An input adapter's read access to the data bag the step is being built from, which it requests
 * like any other request. It has no method that writes, and each read gives the adapter its own
 * copy. Once the call it was given to has returned, it shows nothing: every key reads as absent.
 *
 * @sample org.itineradev.itinera.journey.samples.access
 */
@ExperimentalItineraApi
public class DataBagAccess internal constructor(private val data: DataBag) {
    @Volatile private var open = true

    /**
     * Reads the value under a key as a type, as [DataBag.read] does: [Read.Absent] once the call it
     * was given to has returned.
     *
     * @sample org.itineradev.itinera.journey.samples.access
     */
    public fun <T> read(key: String, type: TypeToken<T>): Read<T> =
        if (open) data.read(key, type) else Read.Absent

    /** Ends the access, once the call it was given to has returned. */
    internal fun close() {
        open = false
    }
}
