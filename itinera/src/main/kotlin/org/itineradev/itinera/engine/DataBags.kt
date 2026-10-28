package org.itineradev.itinera.engine

import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.DataBagAccess

/**
 * Makes data bags, for the engine and for test support.
 *
 * Part of itinera's engine, not of its API.
 */
public object DataBags {
    /** A data bag holding the captured values, under their keys, in the map's order. */
    @JvmStatic
    public fun of(entries: Map<String, Captured>): DataBag =
        DataBag().also { bag -> entries.forEach(bag::put) }

    /** An input adapter's access to [data], open until the engine closes it. */
    @JvmStatic public fun access(data: DataBag): DataBagAccess = DataBagAccess(data)
}
