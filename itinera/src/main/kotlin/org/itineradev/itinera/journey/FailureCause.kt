package org.itineradev.itinera.journey

/**
 * Why a journey failed, each written as the specification names it, such as `retries exhausted`.
 *
 * @sample org.itineradev.itinera.event.samples.failures
 */
public enum class FailureCause(private val text: String) {
    /** The last attempt reported a failure, and the step has no retry budget. */
    FAILURE("failure"),
    /** The step's retry budget, greater than 0, is spent. */
    RETRIES_EXHAUSTED("retries exhausted"),
    /** The last attempt ended in an abnormal termination that is not retried. */
    ABNORMAL_TERMINATION("abnormal termination"),
    /** A hook returned `FailWorkflow`. */
    FAIL_WORKFLOW("FailWorkflow");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
