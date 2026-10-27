package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * What reading the data bag under a key as a type found: [Present], with the reader's own copy of
 * the value, [Absent] when the key holds nothing, or [OtherType] when it holds a value of another
 * type.
 *
 * Kotlin matches a read with `when`, and Java with `switch` or `instanceof`.
 *
 * @param T the type the value was read as.
 * @sample org.itineradev.itinera.journey.samples.reads
 */
@ExperimentalItineraApi
public sealed interface Read<out T> {
    /**
     * The value, when one of the type asked for was found; `null` when the key holds nothing or a
     * value of another type, and when the value found is `null`.
     *
     * @sample org.itineradev.itinera.journey.samples.reads
     */
    public fun orNull(): T?

    /**
     * A value of the type asked for, the reader's own copy, which it may change without changing
     * the data bag.
     *
     * @sample org.itineradev.itinera.journey.samples.reads
     */
    @JvmRecord
    public data class Present<out T>(
        /** The reader's copy of the value. */
        val value: T
    ) : Read<T> {
        override fun orNull(): T = value
    }

    /**
     * Nothing is held under the key.
     *
     * @sample org.itineradev.itinera.journey.samples.reads
     */
    public data object Absent : Read<Nothing> {
        override fun orNull(): Nothing? = null
    }

    /**
     * A value of another type is held under the key.
     *
     * @sample org.itineradev.itinera.journey.samples.reads
     */
    public data object OtherType : Read<Nothing> {
        override fun orNull(): Nothing? = null
    }
}
