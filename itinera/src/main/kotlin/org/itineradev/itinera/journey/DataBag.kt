package org.itineradev.itinera.journey

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Captured
import org.itineradev.itinera.engine.Received
import org.itineradev.itinera.engine.Values
import org.itineradev.itinera.engine.WrongType
import org.itineradev.itinera.value.TypeToken

/**
 * A journey's data: values under string keys, kept in the order each key was first set. It starts
 * with the instance's initial data, changes only when contributions are committed, and is the
 * output of a journey that succeeded or failed.
 *
 * Each read gives the reader its own copy, which it may change without changing the data bag or
 * what any other reader gets.
 *
 * @sample org.itineradev.itinera.journey.samples.reads
 */
@ExperimentalItineraApi
public class DataBag internal constructor() {
    private val values = LinkedHashMap<String, Captured>()

    /**
     * The keys, in the order each was first set.
     *
     * @sample org.itineradev.itinera.journey.samples.keys
     */
    public val keys: Set<String>
        get() = values.keys.toSet()

    /**
     * Whether a value, `null` included, is held under the key.
     *
     * @sample org.itineradev.itinera.journey.samples.reads
     */
    public operator fun contains(key: String): Boolean = key in values

    /**
     * Reads the value under a key as a type: the reader's own copy when it is of that type. Copying
     * a record or data class calls its constructor, and copying a set or a map calls the `equals`
     * and `hashCode` of its elements or keys; when one throws, the value is of another type for
     * this read.
     *
     * @sample org.itineradev.itinera.journey.samples.reads
     */
    public fun <T> read(key: String, type: TypeToken<T>): Read<T> {
        val stored = values[key] ?: return Read.Absent
        return when (val receipt = Values.receive(stored, type)) {
            is Received -> Read.Present(receipt.value)
            is WrongType -> Read.OtherType
        }
    }

    /** Puts a captured value under a key, keeping the key's place when it replaces one. */
    internal fun put(key: String, value: Captured) {
        values[key] = value
    }

    /**
     * The keys and values, in order.
     *
     * @sample org.itineradev.itinera.journey.samples.keys
     */
    override fun toString(): String = values.mapValues(::valueOf).toString()
}

private fun valueOf(entry: Map.Entry<String, Captured>): Any? = entry.value.value
