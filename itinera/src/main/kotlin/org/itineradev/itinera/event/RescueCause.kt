package org.itineradev.itinera.event

/**
 * How the attempt of a step that is rescued failed.
 *
 * @sample org.itineradev.itinera.event.samples.rescued
 */
public enum class RescueCause(private val text: String) {
    /** The attempt reported a failure. */
    FAILURE("failure"),
    /** The attempt ended in an abnormal termination. */
    ABNORMAL_TERMINATION("abnormal termination");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
