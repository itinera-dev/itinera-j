package org.itineradev.itinera.journey

/**
 * What a step or a hook contributes data to the data bag through, made for one attempt or one call
 * and closed when it ends. A step's contributions are committed only when its attempt succeeds; a
 * hook's as soon as it returns.
 *
 * Each value is captured when it is contributed, so changing the object afterwards changes nothing
 * the journey holds, and the last value contributed under a key wins.
 *
 * @sample org.itineradev.itinera.journey.samples.contributors
 */
public fun interface Contributor {
    /**
     * Contributes [value] under [key].
     *
     * @throws IllegalStateException when its attempt or call has ended.
     * @sample org.itineradev.itinera.journey.samples.contributors
     */
    public fun contribute(key: String, value: Any?)
}
