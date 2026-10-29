package org.itineradev.itinera.journey

/**
 * The ID of a journey: a string that is never empty, which every event of the journey carries, and
 * which reporters receive as the global metadata `journey_id`. Two IDs are equal when they spell
 * the same text, which [toString] gives.
 *
 * @sample org.itineradev.itinera.samples.names
 */
public class JourneyId private constructor(private val id: String) {
    /** Whether the other is a journey ID spelling the same text. */
    override fun equals(other: Any?): Boolean = other is JourneyId && other.id == id

    /** A hash of the text, the same for equal IDs. */
    override fun hashCode(): Int = id.hashCode()

    /** The ID's text. */
    override fun toString(): String = id

    /** Makes journey IDs. */
    public companion object {
        /**
         * The journey ID [id].
         *
         * @throws IllegalArgumentException when [id] is empty.
         * @sample org.itineradev.itinera.samples.names
         */
        @JvmStatic
        public fun of(id: String): JourneyId {
            require(id.isNotEmpty()) { "A journey ID is never empty." }
            return JourneyId(id)
        }
    }
}
