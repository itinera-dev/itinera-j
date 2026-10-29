package org.itineradev.itinera.policy

/**
 * Why a step will be attempted again: its attempt reported a failure, or ended in an abnormal
 * termination that the step retries.
 *
 * @sample org.itineradev.itinera.policy.samples.hooks
 */
public enum class RetryCause(private val text: String) {
    /** The attempt reported a failure. */
    FAILURE("failure"),
    /** The attempt ended in an abnormal termination. */
    ABNORMAL_TERMINATION("abnormal termination");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
