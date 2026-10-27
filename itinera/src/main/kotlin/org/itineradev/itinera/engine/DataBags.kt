package org.itineradev.itinera.engine

import org.itineradev.itinera.journey.DataBag

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
}
