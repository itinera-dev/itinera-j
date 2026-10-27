package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * An attempt of a step, by its number, counted from 1: every event about a step carries the attempt
 * it is about. Attempts compare by their numbers.
 *
 * @sample org.itineradev.itinera.step.samples.attempts
 */
@ExperimentalItineraApi
public class Attempt
private constructor(
    /** The attempt's number, counted from 1. */
    public val number: Int
) : Comparable<Attempt> {
    /** Whether this is the step's first attempt. */
    public val isFirst: Boolean
        get() = number == 1

    /** Which of the two attempts came first: below 0 when this one did. */
    override fun compareTo(other: Attempt): Int = number.compareTo(other.number)

    /** Whether the other is an attempt of the same number. */
    override fun equals(other: Any?): Boolean = other is Attempt && other.number == number

    /** The number, which equal attempts share. */
    override fun hashCode(): Int = number

    /** The number, in decimal. */
    override fun toString(): String = number.toString()

    /** Makes attempts. */
    public companion object {
        /**
         * The attempt numbered [number].
         *
         * @throws IllegalArgumentException when [number] is below 1.
         * @sample org.itineradev.itinera.step.samples.attempts
         */
        @JvmStatic
        public fun of(number: Int): Attempt {
            require(number >= 1) { "An attempt is counted from 1, but was given $number." }
            return Attempt(number)
        }
    }
}
