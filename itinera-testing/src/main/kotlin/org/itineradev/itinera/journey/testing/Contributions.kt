package org.itineradev.itinera.journey.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.DataBags
import org.itineradev.itinera.engine.Lone
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.Read
import org.itineradev.itinera.value.TypeToken

/**
 * Lends a contributor outside any journey, so that a test calls a step's run or a hook itself, and
 * keeps what is contributed through it. The contributor behaves as in a journey, keeping a captured
 * copy of each value, the last under a key winning, but nothing is committed: committing a
 * successful attempt's contributions belongs to a journey.
 *
 * @sample org.itineradev.itinera.journey.testing.samples.contributions
 */
@ExperimentalItineraApi
public class Contributions {
    private val lone = Lone(false)

    /**
     * The contributor it lends.
     *
     * @sample org.itineradev.itinera.journey.testing.samples.contributions
     */
    public val contributor: Contributor
        get() = lone.contributor

    /**
     * The keys contributed under, in the order first contributed.
     *
     * @sample org.itineradev.itinera.journey.testing.samples.contributions
     */
    public val keys: Set<String>
        get() = lone.contributed.keys

    /**
     * Reads the last value contributed under [key] as [type], as the data bag's `read` does: the
     * reader's own copy when it is of that type, absent, or of another type.
     *
     * @sample org.itineradev.itinera.journey.testing.samples.contributions
     */
    public fun <T> read(key: String, type: TypeToken<T>): Read<T> =
        DataBags.of(lone.contributed).read(key, type)
}
