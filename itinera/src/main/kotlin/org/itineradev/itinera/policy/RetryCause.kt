package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Why a step will be attempted again: its attempt reported a failure, or ended in an abnormal
 * termination that the step retries.
 *
 * @sample org.itineradev.itinera.policy.samples.hooks
 */
@ExperimentalItineraApi
public enum class RetryCause(private val text: String) {
    /** The attempt reported a failure. */
    FAILURE("failure"),
    /** The attempt ended in an abnormal termination. */
    ABNORMAL_TERMINATION("abnormal termination");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
