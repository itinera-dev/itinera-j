package org.itineradev.itinera

/**
 * The name of a part of a workflow, such as a step: a string that is never empty, in a class of its
 * own for each kind of part, so that a plain string, or another kind of part's name, is not
 * mistaken for it. Two names are equal when they are of the same class and spell the same text,
 * which [toString] gives.
 *
 * @sample org.itineradev.itinera.samples.names
 */
@ExperimentalItineraApi
public abstract class Name internal constructor(private val text: String, part: String) {
    init {
        require(text.isNotEmpty()) { "A $part's name is never empty." }
    }

    /** Whether the other is a name of the same class, spelling the same text. */
    final override fun equals(other: Any?): Boolean =
        other is Name && other.javaClass == javaClass && other.text == text

    /** A hash of the text, the same for equal names. */
    final override fun hashCode(): Int = text.hashCode()

    /** The name's text. */
    final override fun toString(): String = text
}
